package xenit.care4alf.module

import org.springframework.stereotype.Component
import com.github.dynamicextensionsalfresco.webscripts.annotations._
import org.springframework.beans.factory.annotation.Autowired


import xenit.care4alf.spring.ContextAware
import com.typesafe.scalalogging.slf4j.Logging
import com.github.dynamicextensionsalfresco.webscripts.resolutions.{ResolutionParameters, JsonResolution, JsonWriterResolution}
import org.json.{JSONObject, JSONWriter}
import xenit.care4alf.alfresco.HasNodeService

import scala.collection.JavaConversions._
import org.alfresco.service.cmr.action.ActionService
import org.alfresco.service.cmr.repository.NodeRef
import com.github.dynamicextensionsalfresco.webscripts.{AnnotationWebscriptResponse, AnnotationWebScriptRequest}
import org.springframework.extensions.webscripts.WebScriptRequest


/**
 * @author Laurent Van der Linden
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/actions", families = Array("care4alf"), description = "execute actions")
@Authentication(AuthenticationType.ADMIN)
class Actions extends ContextAware with Logging with HasNodeService {

    @Autowired var actionService: ActionService = null

    @Uri(value = Array("/"))
    @Transaction(readOnly = true)
    def list() = new JsonWriterResolution {
        override def writeJson(json: JSONWriter): Unit = {
            json.array()
            val actions = actionService.getActionDefinitions
            for (action <- actions) {
                json
                    .`object`()
                        .key("name").value(action.getName)
                        .key("description").value(action.getDescription)
                        .key("title").value(action.getTitle)
                    .endObject()
            }
            json.endArray()
        }
    }

    @Uri(value = Array("/{name}/run"), method = HttpMethod.POST)
    def update(@UriVariable name: String, request: WebScriptRequest) {
        try {
            val content = request.getContent.getContent
            val body = if ("" == content) new JSONObject() else new JSONObject(content)
            val action = actionService.createAction(name)
            val noderef = if (body.has("noderef")) new NodeRef(body.getString("noderef")) else null
            actionService.executeAction(action, noderef, false, false)
        }
        catch {
            case exception: Throwable => new JsonResolution(500) {
                override def resolve(request: AnnotationWebScriptRequest, response: AnnotationWebscriptResponse, params: ResolutionParameters): Unit = {
                    super.resolve(request, response, params)
                    new JSONWriter(response.getWriter)
                        .`object`()
                            .key("message").value(exception.getMessage)
                        .endObject()
                }
            }
        }
    }
}
