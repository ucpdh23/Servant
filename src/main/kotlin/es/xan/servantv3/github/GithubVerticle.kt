package es.xan.servantv3.github

import com.google.common.cache.CacheBuilder
import es.xan.servantv3.*
import es.xan.servantv3.ThrowingUtils.retry3times
import es.xan.servantv3.homeautomation.HomeVerticle
import es.xan.servantv3.messages.JiraMessage
import es.xan.servantv3.messages.TextMessage
import es.xan.servantv3.messages.TextMessageToTheBoss
import es.xan.servantv3.messages.VersionInfo
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.http.HttpEntity
import org.apache.http.client.config.CookieSpecs
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap


class GithubVerticle: AbstractServantVerticle(Constant.GITHUB_VERTICLE) {
    companion object {
        val LOG = LoggerFactory.getLogger(AzureDevOpsVerticle::class.java.name)

        val HTTP_CLIENT = HttpClients.custom()
            .setDefaultRequestConfig(
                RequestConfig.custom()
                .setCookieSpec(CookieSpecs.STANDARD)
                    .setConnectTimeout(10000)
                    .setSocketTimeout(90000)
                .build())
            .build()

        val CACHE = CacheBuilder.newBuilder()
            .maximumSize(50)
            .expireAfterWrite(10, TimeUnit.DAYS)
            .build<String, String>()
    }

    private var currentVersions: MutableMap<String, VersionInfo> = HashMap();
    private var mConfiguration: JsonObject? = null

    private val GITHUB_API_URL = "https://api.github.com"

    init {
        LOG.info("loading GithubVerticle...")
        supportedActions(Actions::class.java)
        initDatabase()
        LOG.info("loaded GithubVerticle")
    }

    fun initDatabase() {
        App.connection.createStatement().use { statement ->
            LOG.info("updating servant_versions...")
            statement.executeUpdate("""
                create table if not exists servant_versions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    tagname string,
                    filename string,
                    url string,
                    status string
                )""".trimIndent());

            val resultSet = statement.executeQuery("PRAGMA table_info(servant_versions)")
            val columnExists = generateSequence {
                if (resultSet.next()) resultSet.getString("name") else null
            }.any { it.equals("project", ignoreCase = true) }

            // Add 'project' column if it doesn't exist
            if (!columnExists) {
                statement.executeUpdate("ALTER TABLE servant_versions ADD COLUMN project TEXT")
                statement.executeUpdate("UPDATE servant_versions SET project = 'servant'")
            }

            LOG.info("updated servant_versions")
        }
    }

    override fun start() {
        super.start();

        this.mConfiguration = Vertx.currentContext().config().getJsonObject("GithubVerticle")
        val projects = this.mConfiguration?.getJsonArray("projects");

        if (projects == null) {
            LOG.warn("Cannot initialize projects from configuration object");
            return
        };

        for (project in projects.list) {
            val jsonProject = project as JsonObject

            if (jsonProject.getBoolean("updatedVersionRequired")) {
                vertx.setTimer(60000) { _ ->
                    publishAction(Actions.CHECK_UPDATED_VERSION, jsonProject)
                }
            }

            vertx.setPeriodic(600000) { _ ->
                publishAction(Actions.CHECK_NEW_VERSION, jsonProject);
            }

        }


    }

    // *********** Supported actions

    enum class Actions(val clazz : Class<*>? ) : Action {
        /**
         * Checks the current status the all the available devices
         */
        CHECK_NEW_VERSION(null),
        CHECK_UPDATED_VERSION(null),
        UPDATE_VERSION(TextMessage::class.java),
        FETCH_OPEN_ISSUES(null),
        ADD_COMMENT_TO_ISSUE(JiraMessage::class.java),
        FETCH_ISSUE_DETAILS(JiraMessage::class.java);

    }

    fun fetch_issue_details(message: JiraMessage, msg: Message<Any>) {
        LOG.info("Get details to issue #{} in repository [{}]", message.issueNumber)

        val account = this.mConfiguration!!.getString("account")
        val repository = this.mConfiguration!!.getString("targetRepository")

        val details = getIssueDetailsWithComments(account, repository, message.issueNumber)

        val reply = MessageBuilder.createReply().apply {
            setOk()
            setMessage(details.getString("comments").toString())
        }

        msg.reply(reply.build())
    }

    fun getIssueDetailsWithComments(account: String, repository: String, issueNumber: Int): JsonObject {
        val accessToken = this.mConfiguration!!.getString("projectBuilderAccessToken") // GitHub Personal Access Token

        // API endpoints
        val issueUrl = "$GITHUB_API_URL/repos/$account/$repository/issues/$issueNumber"
        val commentsUrl = "$GITHUB_API_URL/repos/$account/$repository/issues/$issueNumber/comments"

        val issueRequest = HttpGet(issueUrl)
        val commentsRequest = HttpGet(commentsUrl)

        // Add Authorization headers with Bearer token for both requests
        issueRequest.addHeader("Authorization", "Bearer $accessToken")
        issueRequest.addHeader("Accept", "application/vnd.github.v3+json")

        commentsRequest.addHeader("Authorization", "Bearer $accessToken")
        commentsRequest.addHeader("Accept", "application/vnd.github.v3+json")

        LOG.info("Fetching full issue details for issue #{} in repository [{}]", issueNumber, repository)

        // First: Fetch issue details
        var issueDetails = JsonObject()
        HTTP_CLIENT.execute(issueRequest).use { response ->
            val statusCode = response.statusLine.statusCode
            if (statusCode == 200) {
                val entity = response.entity
                val jsonString = EntityUtils.toString(entity)
                issueDetails = JsonObject(jsonString)

                LOG.info("Successfully fetched issue details for issue #{}", issueNumber)
            } else {
                LOG.error("Failed to fetch issue details. Status code: {}", statusCode)
                throw ServantException("Failed to fetch issue details for issue #$issueNumber")
            }
        }

        // Second: Fetch issue comments
        val commentsArray = JsonArray()
        HTTP_CLIENT.execute(commentsRequest).use { response ->
            val statusCode = response.statusLine.statusCode
            if (statusCode == 200) {
                val entity = response.entity
                val jsonString = EntityUtils.toString(entity)
                val comments = JsonArray(jsonString)

                LOG.info("Successfully fetched {} comments for issue #{}", comments.size(), issueNumber)

                // Add each comment to the commentsArray
                for (i in 0 until comments.size()) {
                    val comment = comments.getJsonObject(i)
                    commentsArray.add(comment)
                }
            } else {
                LOG.error("Failed to fetch comments for issue #{}. Status code: {}", issueNumber, statusCode)
            }
        }

        // Attach the comments array to the issue details
        issueDetails.put("comments", commentsArray)

        return issueDetails
    }

    fun add_comment_to_issue(message : JiraMessage, msg : Message<Any>) {
        LOG.info("Adding comment to issue #{} in repository [{}]", message.issueNumber)

        val result = this.addCommentToIssue(message)

        val reply = MessageBuilder.createReply().apply {
            if (result) setOk()
            else setError()

            setMessage(message.toString())
        }

        msg.reply(reply.build())
    }

    private fun addCommentToIssue(message : JiraMessage) : Boolean {
        val accessToken = this.mConfiguration!!.getString("projectBuilderAccessToken") // GitHub Personal Access Token
        val account = this.mConfiguration!!.getString("account")
        val repository = this.mConfiguration!!.getString("targetRepository")

        val url = "$GITHUB_API_URL/repos/$account/$repository/issues/${message.issueNumber}/comments"
        val request = HttpPost(url)

        // Add Authorization header with Bearer token
        request.addHeader("Authorization", "Bearer $accessToken")
        request.addHeader("Accept", "application/vnd.github.v3+json")

        val issueComment = "${message.agent}:\n${message.message}"

        // Create JSON payload with the comment
        val commentJson = JsonObject().put("body", issueComment).toString()
        request.entity = StringEntity(commentJson, ContentType.APPLICATION_JSON)

        LOG.info("Adding comment to issue #{} in repository [{}]", message.issueNumber, repository)

        HTTP_CLIENT.execute(request).use { response ->
            val statusCode = response.statusLine.statusCode
            if (statusCode == 201) {  // 201 Created indicates success
                LOG.info("Comment successfully added to issue #{}", message.issueNumber)
                return true
            } else {
                LOG.error("Failed to add comment to issue #{}. Status code: {}", message.issueNumber, statusCode)
            }
        }
        return false

    }

    fun fetch_open_issues(message : Message<Any>) {
        val issues = fetchOpenIssues(this.mConfiguration!!.getString("account"), this.mConfiguration!!.getString("targetRepository"))

        val reply = MessageBuilder.createReply().apply {
            setOk()
            setMessage(issues.toString())
        }

        message.reply(reply.build())
    }

    // ********* New Method to fetch Open GitHub Issues
    fun fetchOpenIssues(account: String, repository: String): List<JsonObject> {
        val accessToken = this.mConfiguration!!.getString("projectBuilderAccessToken") // GitHub Personal Access Token
        val url = "$GITHUB_API_URL/repos/$account/$repository/issues?state=open"
        val request = HttpGet(url)

        request.addHeader("Authorization", "Bearer $accessToken")  // Add authorization header
        request.addHeader("Accept", "application/vnd.github.v3+json")

        LOG.info("Fetching open issues from GitHub [{}]", url)

        HTTP_CLIENT.execute(request).use { response ->
            val statusCode = response.statusLine.statusCode
            if (statusCode == 200) {
                val entity: HttpEntity = response.entity
                val jsonString = EntityUtils.toString(entity)
                val jsonIssues = JsonArray(jsonString)

                LOG.info("Fetched {} open issues", jsonIssues.size())

                val issuesList = mutableListOf<JsonObject>()
                for (i in 0 until jsonIssues.size()) {
                    val issue = jsonIssues.getJsonObject(i)
                    issuesList.add(issue)
                }
                return issuesList
            } else {
                LOG.error("Failed to fetch issues. Status code: {}", statusCode)
            }
        }
        return emptyList()
    }

    fun check_updated_version(project : JsonObject) {
        LOG.info("Check current version...")

        App.connection.createStatement().use { statement ->
            val projectName = project.getString("name")
            var tagname = "not_found"
            var url = "not_found"
            var filename = "not_found"
            var status = "not_found"

            statement.executeQuery("SELECT * FROM servant_versions WHERE project = '${projectName}' ORDER BY id DESC LIMIT 1").use {  rs ->
                while (rs.next()) {
                    tagname = rs.getString("tagname")
                    filename = rs.getString("filename")
                    url = rs.getString("url")
                    status = rs.getString("status")
                }
            }

            LOG.info("Found current version [{}-{}-{}-{}]", tagname, filename, url, status)

            if (status == "prepared") {
                statement.executeUpdate("UPDATE servant_versions set status = 'installed' where tagname ='$tagname' AND project = '${projectName}'")
                publishAction(HomeVerticle.Actions.NOTIFY_BOSS, TextMessageToTheBoss("Installed version $tagname for project $projectName"))
            }

            this.currentVersions[projectName] = VersionInfo(filename, tagname, url)
            LOG.debug("Loaded current version [{}->{}-{}-{}] [{}]", projectName,
                this.currentVersions[projectName]?.filename,
                this.currentVersions[projectName]?.tagName,
                this.currentVersions[projectName]?.url,
                status)
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

    fun check_new_version(project : JsonObject) {
        val projectName = project.getString("name")

        val lastVersion = resolveLastVersionInfo(project.getString("account"), project.getString("repository"))
        val cacheKey = projectName + "-" + lastVersion.tagName
        val requested = CACHE.get(cacheKey) { -> "NO" }
        LOG.debug("[{}]->{}", lastVersion.tagName, requested)

        if (lastVersion.tagName != this.currentVersions[projectName]?.tagName && requested == "NO") {
            CACHE.put(cacheKey, "YES")

            publishEvent(Events.NEW_VERSION_AVAILABLE, lastVersion)
            publishAction(HomeVerticle.Actions.NOTIFY_BOSS, TextMessageToTheBoss("new version '${lastVersion.tagName}' available for project '$projectName' Do you want to install it?"))
        }
    }

    fun update_version(msg : TextMessage) {
        if (msg.message.split("\t")[1].lowercase().contains("yes")) {
            LOG.info("Updating version from remote repository")

            val expectedTagName = msg.message.split("'")[1]
            val expectedProjectName = msg.message.split("'")[3]
            val project = resolveProject(expectedProjectName)
            if (project == null) {
                LOG.warn("Cannot identify project $expectedProjectName")
                return
            }

            val location = project.getString("location")
            val lastVersion = resolveLastVersionInfo(project.getString("account"), project.getString("repository"))

            if (expectedTagName == lastVersion.tagName) {
                GlobalScope.launch {
                    LOG.info("Matches tagname: [{}]", expectedTagName)
                    val downloaded = downloadVersion(lastVersion, location)
                    if (downloaded) {
                        addToDatabase(expectedProjectName, lastVersion)
                        rebootServer(project)
                    } else {
                        LOG.warn("problems downloading version");
                    }
                }
            } else {
                LOG.warn("Unexpected Tagname [{}-{}]", expectedTagName, lastVersion.tagName)
            }

        }
    }

    private fun resolveProject(name : String) : JsonObject? {
        val projects = this.mConfiguration?.getJsonArray("projects") ?: return null;

        for (project in projects) {
            if ((project as JsonObject).getString("name") == name)
                return project
        }

        return null;
    }

    private fun addToDatabase(projectName : String, version : VersionInfo) {
        App.connection.createStatement().use { statement ->
            statement.executeUpdate("""
                insert into servant_versions
                    (project, tagname, filename, url, status)
                VALUES
                    ('$projectName', '${version.tagName}', '${version.filename}', '${version.url}', 'prepared');"""
            )
        }
    }

    private fun rebootServer(project : JsonObject) {
        LOG.warn("requesting to reboot server")

        SSHUtils.runRemoteCommand(
            project.getString("host"),
            project.getString("user"),
            project.getString("pwd"),
            project.getString("command")
        )

        if (!project.getBoolean("updatedVersionRequired")) {
            publishAction(Actions.CHECK_UPDATED_VERSION, project)
        }
    }

    private fun downloadVersion(version: VersionInfo, destination : String) : Boolean {
        LOG.info("downloading version [{}]", version.tagName)
        val request = HttpGet(version.url)
        val output = retry3times({ ->
            HTTP_CLIENT.execute(request).use { response ->
                if (response.statusLine.statusCode == 200) {
                    val entity: HttpEntity = response.entity
                    val inputStream = entity.content

                    inputStream.use { input ->
                        val tempFile = File.createTempFile(version.tagName, "bin")
                        val bytes = Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

                        Files.copy(tempFile.toPath(), File(destination).toPath(), StandardCopyOption.REPLACE_EXISTING)

                        tempFile.delete()

                        LOG.debug("downloaded [{}] bytes", bytes)
                    }
                }
            }

            true},
            { x -> x});

        if (output == null) return false;
        return output;
    }

}