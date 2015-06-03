package eu.xenit.care4alf.module

import org.springframework.stereotype.Component
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript
import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType
import eu.xenit.care4alf.json
import org.springframework.beans.factory.annotation.Autowired
import com.github.dynamicextensionsalfresco.webscripts.annotations.Transaction
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri
import org.json.JSONWriter
import com.github.dynamicextensionsalfresco.webscripts.annotations.HttpMethod
import org.springframework.extensions.webscripts.WebScriptRequest
import com.github.dynamicextensionsalfresco.webscripts.annotations.UriVariable
import com.github.dynamicextensionsalfresco.webscripts.resolutions.JsonResolution
import com.github.dynamicextensionsalfresco.webscripts.resolutions.Resolution
import org.json.JSONObject
import com.github.dynamicextensionsalfresco.webscripts.resolutions.ErrorResolution
import org.alfresco.service.cmr.repository.NodeRef
import org.alfresco.service.cmr.action.ActionService
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationContext
import org.slf4j.LoggerFactory
import eu.xenit.care4alf.web.WebscriptDefaults
import eu.xenit.care4alf.web.LogHelper

/**
 * @author Laurent Van der Linden
 */
Component
WebScript(baseUri = "/xenit/care4alf/actions", families = arrayOf("care4alf"), description = "execute actions")
Authentication(AuthenticationType.ADMIN)
class Actions @Autowired constructor(
        private val actionService: ActionService
        ) : LogHelper, WebscriptDefaults {
    override val logger = LoggerFactory.getLogger(javaClass)

    Uri(value = "/")
    Transaction(readOnly = true)
    fun list() = json {
        iterable(actionService.getActionDefinitions()) { action ->
            obj {
                entry("name", action.getName())
                entry("description", action.getDescription())
                entry("title", action.getTitle())
            }
        }
    }

    Uri(value = "/{name}/run", method = HttpMethod.POST)
    fun run(UriVariable name: String, body: JSONObject) {
        val action = actionService.createAction(name)
        val noderef = if (body.has("noderef")) NodeRef(body.getString("noderef")) else null
        actionService.executeAction(action, noderef, false, false)
    }
}
