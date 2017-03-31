package eu.xenit.care4alf.module

import com.github.dynamicextensionsalfresco.webscripts.annotations.*
import eu.xenit.care4alf.json
import eu.xenit.care4alf.web.LogHelper
import eu.xenit.care4alf.web.WebscriptDefaults
import org.alfresco.service.cmr.action.ActionService
import org.alfresco.service.cmr.repository.NodeRef
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * @author Laurent Van der Linden
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/actions", families = arrayOf("care4alf"), description = "execute actions")
@Authentication(AuthenticationType.ADMIN)
class Actions @Autowired constructor(
        private val actionService: ActionService
        ) : LogHelper(), WebscriptDefaults {
    override val logger = LoggerFactory.getLogger(javaClass)

    @Uri(value = "/")
    @Transaction(readOnly = true)
    fun list() = json {
        iterable(actionService.getActionDefinitions()) { action ->
            obj {
                entry("name", action.getName())
                entry("description", action.getDescription())
                entry("title", action.getTitle())
            }
        }
    }

    @Uri(value = "/{name}/run", method = HttpMethod.POST)
    fun run(@UriVariable name: String, body: JSONObject) {
        val action = actionService.createAction(name)
        val noderef = if (body.has("noderef")) NodeRef(body.getString("noderef")) else null
        actionService.executeAction(action, noderef, false, false)
    }
}
