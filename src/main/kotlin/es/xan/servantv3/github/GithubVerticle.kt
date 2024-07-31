package es.xan.servantv3.github

import com.google.common.cache.CacheBuilder
import es.xan.servantv3.*
import es.xan.servantv3.homeautomation.HomeVerticle
import es.xan.servantv3.messages.TextMessage
import es.xan.servantv3.messages.TextMessageToTheBoss
import es.xan.servantv3.messages.VersionInfo
import es.xan.servantv3.network.NetworkVerticle
import es.xan.servantv3.parrot.ParrotVerticle
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.http.HttpEntity
import org.apache.http.client.config.CookieSpecs
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.concurrent.TimeUnit


class GithubVerticle: AbstractServantVerticle(Constant.GITHUB_VERTICLE) {
    companion object {
        val LOG = LoggerFactory.getLogger(GithubVerticle::class.java.name)

        val HTTP_CLIENT = HttpClients.custom()
            .setDefaultRequestConfig(
                RequestConfig.custom()
                .setCookieSpec(CookieSpecs.STANDARD)
                    .setConnectTimeout(10000)
                    .setSocketTimeout(30000)
                .build())
            .build()

        val CACHE = CacheBuilder.newBuilder()
            .maximumSize(50)
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build<String, String>()
    }

    private var currentVersion: VersionInfo = VersionInfo("","","")
    private var mConfiguration: JsonObject? = null

    init {
        LOG.info("loading GithubVerticle...")
        supportedActions(Actions::class.java)
        initDatabase()
        LOG.info("loaded GithubVerticle")
    }

    fun initDatabase() {
        App.connection.createStatement().use { statement ->
            statement.executeUpdate("create table if not exists servant_versions (id INTEGER PRIMARY KEY AUTOINCREMENT, tagname string, filename string, url string, status string)")
        }
    }

    override fun start() {
        super.start();

        this.mConfiguration = Vertx.currentContext().config().getJsonObject("GithubVerticle")

        vertx.setTimer(60000) { _ ->
            publishAction(Actions.CHECK_UPDATED_VERSION)
        }

        vertx.setPeriodic(600000) { _ ->
            publishAction(Actions.CHECK_NEW_VERSION);
        }
    }

    // *********** Supported actions

    enum class Actions(val clazz : Class<*>? ) : Action {
        /**
         * Checks the current status the all the available devices
         */
        CHECK_NEW_VERSION(null),
        CHECK_UPDATED_VERSION(null),
        UPDATE_VERSION(TextMessage::class.java);
    }

    fun check_updated_version() {
        LOG.info("Check current version...")

        App.connection.createStatement().use { statement ->
            var tagname = "not_found"
            var url = "not_found"
            var filename = "not_found"
            var status = "not_found"

            statement.executeQuery("SELECT * FROM servant_versions ORDER BY id DESC LIMIT 1").use {  rs ->
                while (rs.next()) {
                    tagname = rs.getString("tagname")
                    filename = rs.getString("filename")
                    url = rs.getString("url")
                    status = rs.getString("status")
                }
            }

            LOG.info("Found current version [{}-{}-{}-{}]", tagname, filename, url, status)

            if (status == "prepared") {
                statement.executeUpdate("UPDATE servant_versions set status = 'installed' where tagname ='$tagname'")
                publishAction(HomeVerticle.Actions.NOTIFY_BOSS, TextMessageToTheBoss("Installed version $tagname"))
            }

            this.currentVersion = VersionInfo(filename, tagname, url)
            LOG.debug("Loaded current version [{}-{}-{}] [{}]", this.currentVersion.filename, this.currentVersion.tagName, this.currentVersion.url, status)
        }

    }

    fun resolveLastVersionInfo(account: String, repository: String) : VersionInfo {
        val request: HttpGet = HttpGet("https://api.github.com/repos/$account/$repository/releases/latest")
        LOG.info("request [{}]", request)

        HTTP_CLIENT.execute(request).use { response ->
            if (response.statusLine.statusCode == 200) {
                val entity: HttpEntity = response.entity
                val jsonString = EntityUtils.toString(entity)
                val output = JsonObject(jsonString)

                val tagName = output.getString("tag_name");
                val filename = output.getJsonArray("assets").getJsonObject(0).getString("name")
                val versionUrl = output.getJsonArray("assets").getJsonObject(0).getString("browser_download_url")

                LOG.info("Last version in remote repo [{}-{}-{}]", tagName, filename, versionUrl)

                return VersionInfo(filename, tagName, versionUrl)
            }
        }

        throw ServantException()
    }

    fun check_new_version() {
        val lastVersion = resolveLastVersionInfo(this.mConfiguration!!.getString("account"), this.mConfiguration!!.getString("repository"))
        val requested = CACHE.get(lastVersion.tagName) { -> "NO" }
        LOG.debug("[{}]->{}", lastVersion.tagName, requested)

        if (lastVersion.tagName != this.currentVersion.tagName && requested == "NO") {
            CACHE.put(lastVersion.tagName, "YES")

            publishEvent(Events.NEW_VERSION_AVAILABLE, lastVersion)
            publishAction(HomeVerticle.Actions.NOTIFY_BOSS, TextMessageToTheBoss("new version '${lastVersion.tagName}' available. Do you want to install it?"))
        }
    }

    fun update_version(msg : TextMessage) {
        if (msg.message.split("\t")[1].lowercase().contains("yes")) {
            LOG.info("Updating version from remote repository")

            val expectedTagName = msg.message.split("'")[1]
            val location = this.mConfiguration!!.getString("location")

            val lastVersion = resolveLastVersionInfo(this.mConfiguration!!.getString("account"), this.mConfiguration!!.getString("repository"))

            if (expectedTagName == lastVersion.tagName) {
                GlobalScope.launch {
                    LOG.info("Matches tagname: [{}]", expectedTagName)
                    val downloaded = downloadVersion(lastVersion, location)
                    if (downloaded) {
                        addToDatabase(lastVersion)
                        rebootServer()
                    } else {
                        LOG.warn("problems downloading version");
                    }
                }
            } else {
                LOG.warn("Unexpected Tagname [{}-{}]", expectedTagName, lastVersion.tagName)
            }

        }
    }

    fun addToDatabase(version : VersionInfo) {
        App.connection.createStatement().use { statement ->
            statement.executeUpdate("insert into servant_versions (tagname, filename, url, status) VALUES ('${version.tagName}', '${version.filename}', '${version.url}', 'prepared');")
        }
    }

    fun rebootServer() {
        LOG.warn("requesting to reboot server")

        SSHUtils.runRemoteCommand(
            this.mConfiguration!!.getString("host"),
            this.mConfiguration!!.getString("user"),
            this.mConfiguration!!.getString("pwd"),
            this.mConfiguration!!.getString("command")
        )
    }

    fun downloadVersion(version: VersionInfo, destination : String) : Boolean {
        LOG.info("downloading version [{}]", version.tagName)
        val request = HttpGet(version.url)
        try {
            HTTP_CLIENT.execute(request).use { response ->
                if (response.statusLine.statusCode == 200) {
                    val entity: HttpEntity = response.entity
                    val inputStream = entity.content

                    inputStream.use { input ->
                        val bytes = Files.copy(input, File(destination).toPath(), StandardCopyOption.REPLACE_EXISTING)
                        LOG.debug("downloaded [{}] bytes", bytes)
                    }
                }

                return true
            }
        } catch (e : Throwable) {
            LOG.warn(e.message, e)
        }

        return false
    }

}