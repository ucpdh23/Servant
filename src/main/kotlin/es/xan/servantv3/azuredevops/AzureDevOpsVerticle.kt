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
import kotlinx.serialization.json.buildJsonObject

import io.vertx.core.json.JsonObject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.put
import org.apache.http.HttpEntity
import org.apache.http.client.config.CookieSpecs
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPatch
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


class AzureDevOpsVerticle: AbstractServantVerticle(Constant.AZUREDEVOPS_VERTICLE) {
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
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build<String, String>()
    }

    private var mConfiguration: JsonObject? = null

    private val AZURE_DEVOPS_API_URL = "https://dev.azure.com"

    init {
        LOG.info("loading AzureDevOpsVerticle...")
        supportedActions(Actions::class.java)
        LOG.info("loaded AzureDevOpsVerticle")
    }



    override fun start() {
        super.start();

        this.mConfiguration = Vertx.currentContext().config().getJsonObject("AzureDevOpsVerticle")


    }

    // *********** Supported actions

    enum class Actions(val clazz : Class<*>? ) : Action {
        /**
         * Checks the current status the all the available devices
         */
        FETCH_OPEN_WORK_ITEMS(null),
        ADD_COMMENT_TO_WORK_ITEM(JiraMessage::class.java),
        ASSIGN_WORK_ITEM_TO_USER(JiraMessage::class.java),
        FETCH_WORK_ITEM_DETAILS(JiraMessage::class.java);
    }

    fun assign_work_item_to_user(message: JiraMessage, msg: Message<Any>) {
        val workItemId = message.issueNumber

        val organization = this.mConfiguration!!.getString("organization")
        val project = this.mConfiguration!!.getString("project")
        val accessToken = this.mConfiguration!!.getString("accessToken_b64") // Azure DevOps Personal Access Token


        val workItemUrl = "https://dev.azure.com/$organization/$project/_apis/wit/workitems/${workItemId}?api-version=6.0"

        // Create JSON body
        val jsonBody = buildJsonObject {
            put("op", "add")
            put("path", "/fields/System.AssignedTo")
            put("value", message.message)
        }

        val workItemPatch = HttpPatch(workItemUrl)

        workItemPatch.addHeader("Authorization", "Bearer $accessToken")
        workItemPatch.addHeader("Accept", "application/json")

        workItemPatch.entity = StringEntity(jsonBody.toString())

        HTTP_CLIENT.execute(workItemPatch).use { response ->
            val statusCode = response.statusLine.statusCode
            if (statusCode == 200) {
                val entity = response.entity

                LOG.info("Successfully fetched work item details for #{}", workItemId)
            } else {
                LOG.error("Failed to fetch work item details. Status code: {}", statusCode)
                throw ServantException("Failed to fetch work item details for #$workItemId")
            }
        }

        val reply = MessageBuilder.createReply().apply {
            setOk()
            setMessage("")
        }

        msg.reply(reply.build())

    }

    fun fetch_work_item_details(message: JiraMessage, msg: Message<Any>) {
        LOG.info("Get details for work item #{} in repository [{}]", message.issueNumber)

        val organization = this.mConfiguration!!.getString("organization")
        val project = this.mConfiguration!!.getString("project")

        val details = getWorkItemDetailsWithComments(organization, project, message.issueNumber)

        val reply = MessageBuilder.createReply().apply {
            setOk()
            setMessage(details.getString("comments").toString())
        }

        msg.reply(reply.build())
    }

    // Method to retrieve work item details and comments
    fun getWorkItemDetailsWithComments(organization: String, project: String, workItemId: Int): JsonObject {
        val accessToken = this.mConfiguration!!.getString("accessToken_b64") // Azure DevOps Personal Access Token

        val workItemUrl = "$AZURE_DEVOPS_API_URL/$organization/$project/_apis/wit/workitems/$workItemId?api-version=7.0"
        val commentsUrl = "$AZURE_DEVOPS_API_URL/$organization/$project/_apis/wit/workitems/$workItemId/comments?api-version=7.0"

        val workItemRequest = HttpGet(workItemUrl)
        val commentsRequest = HttpGet(commentsUrl)

        // Add Authorization headers with Bearer token for both requests
        workItemRequest.addHeader("Authorization", "Basic $accessToken")
        workItemRequest.addHeader("Accept", "application/json")

        commentsRequest.addHeader("Authorization", "Bearer $accessToken")
        commentsRequest.addHeader("Accept", "application/json")

        LOG.info("Fetching full work item details for #{} in project [{}]", workItemId, project)

        var workItemDetails = JsonObject()
        HTTP_CLIENT.execute(workItemRequest).use { response ->
            val statusCode = response.statusLine.statusCode
            if (statusCode == 200) {
                val entity = response.entity
                val jsonString = EntityUtils.toString(entity)
                workItemDetails = JsonObject(jsonString)

                LOG.info("Successfully fetched work item details for #{}", workItemId)
            } else {
                LOG.error("Failed to fetch work item details. Status code: {}", statusCode)
                throw ServantException("Failed to fetch work item details for #$workItemId")
            }
        }

        val commentsArray = JsonArray()
        HTTP_CLIENT.execute(commentsRequest).use { response ->
            val statusCode = response.statusLine.statusCode
            if (statusCode == 200) {
                val entity = response.entity
                val jsonString = EntityUtils.toString(entity)
                val comments = JsonArray(jsonString)

                LOG.info("Successfully fetched {} comments for work item #{}", comments.size(), workItemId)

                for (i in 0 until comments.size()) {
                    val comment = comments.getJsonObject(i)
                    commentsArray.add(comment)
                }
            } else {
                LOG.error("Failed to fetch comments for work item #{}. Status code: {}", workItemId, statusCode)
            }
        }

        workItemDetails.put("comments", commentsArray)
        return workItemDetails
    }


    // Method to add a comment to a work item in Azure DevOps
    fun add_comment_to_work_item(message: JiraMessage, msg: Message<Any>) {
        LOG.info("Adding comment to work item #{} in project [{}]", message.issueNumber)

        val result = this.addCommentToWorkItem(message)

        val reply = MessageBuilder.createReply().apply {
            if (result) setOk()
            else setError()

            setMessage(message.toString())
        }

        msg.reply(reply.build())
    }

    private fun addCommentToWorkItem(message: JiraMessage): Boolean {
        val accessToken = this.mConfiguration!!.getString("accessToken_b64")
        val organization = this.mConfiguration!!.getString("organization")
        val project = this.mConfiguration!!.getString("project")

        val url = "$AZURE_DEVOPS_API_URL/$organization/$project/_apis/wit/workitems/${message.issueNumber}/comments?api-version=7.0"
        val request = HttpPost(url)

        request.addHeader("Authorization", "Basic $accessToken")
        request.addHeader("Accept", "application/json")

        val workItemComment = "${message.agent}:\n${message.message}"
        val commentJson = JsonObject().put("text", workItemComment).toString()
        request.entity = StringEntity(commentJson, ContentType.APPLICATION_JSON)

        LOG.info("Adding comment to work item #{} in project [{}]", message.issueNumber, project)

        HTTP_CLIENT.execute(request).use { response ->
            val statusCode = response.statusLine.statusCode
            if (statusCode == 201) {  // 201 Created indicates success
                LOG.info("Comment successfully added to work item #{}", message.issueNumber)
                return true
            } else {
                LOG.error("Failed to add comment to work item #{}. Status code: {}", message.issueNumber, statusCode)
            }
        }
        return false
    }

    // Fetch open work items in Azure DevOps
    fun fetch_open_work_items(message: Message<Any>) {
        val workItems = fetchOpenWorkItems(
            this.mConfiguration!!.getString("organization"),
            this.mConfiguration!!.getString("project"),
            mapOf("[System.State]" to "'To Do'")

        )

        val reply = MessageBuilder.createReply().apply {
            setOk()
            setMessage(workItems.toString())
        }

        message.reply(reply.build())
    }

    fun fetchOpenWorkItems(organization: String, project: String, conditions: Map<String, String>): List<JsonObject> {
        val accessToken = this.mConfiguration!!.getString("accessToken_b64") // Azure DevOps Personal Access Token

        // Azure DevOps REST API endpoint to fetch open work items
        val url = "https://dev.azure.com/$organization/$project/_apis/wit/wiql?api-version=6.0"

        val request = HttpPost(url)

        // Add authorization header with Bearer token
        request.addHeader("Authorization", "Basic $accessToken")
        request.addHeader("Content-Type", "application/json")

        var str_conditions = "";
        for (entry in conditions.entries) {
            str_conditions += " And "
            str_conditions += entry.key + " = " + entry.value
        }


        // The WIQL query to fetch open work items
        val wiqlQuery = """
        {
            "query": "Select [System.Id], [System.Title], [System.State] From WorkItems Where [System.TeamProject] = '$project' $str_conditions"
        }
    """
        request.entity = StringEntity(wiqlQuery, ContentType.APPLICATION_JSON)

        LOG.info("Fetching open work items from Azure DevOps [{}]", url)

        val workItemsList = mutableListOf<JsonObject>()

        HTTP_CLIENT.execute(request).use { response ->
            val statusCode = response.statusLine.statusCode
            if (statusCode == 200) {
                val entity = response.entity
                val jsonString = EntityUtils.toString(entity)
                val jsonResponse = JsonObject(jsonString)

                // Parse the list of work items
                val workItems = jsonResponse.getJsonArray("workItems")

                LOG.info("Fetched {} open work items", workItems.size())

                // For each work item, fetch the detailed information
                for (i in 0 until workItems.size()) {
                    val workItemId = workItems.getJsonObject(i).getInteger("id")
                    val workItemDetails = getWorkItemDetailsWithComments(organization, project, workItemId)
                    workItemsList.add(workItemDetails)
                }
            } else {
                LOG.error("Failed to fetch work items. Status code: {}", statusCode)
            }
        }

        return workItemsList
    }

}