package xenit.care4alf.web

import org.springframework.extensions.webscripts.WebScriptResponse
import nl.runnable.alfresco.webscripts.annotations.Attribute
import org.json.JSONWriter
import org.alfresco.service.cmr.repository.{NodeService, NodeRef}
import scala.collection.JavaConversions._

/**
 * @author Laurent Van der Linden
 */
trait Json {
    @Attribute
    def jsonHelper(response: WebScriptResponse) = new JsonHelper(response)
}

class JsonHelper(response: WebScriptResponse) {
    lazy val json = {
        response.setContentType("application/json")
        response.setContentEncoding("utf-8")
        response.setHeader("Cache-Control", "no-cache")
        new JSONWriter(response.getWriter)
    }
}

object Json {
    implicit def nodeToJsonNode(nodeRef: NodeRef) = new RichJsonNodeType(nodeRef)
}

class RichJsonNodeType(val nodeRef: NodeRef) extends AnyVal {
    def toJson(nodeService: NodeService)(implicit json: JSONWriter) = {
        val properties = nodeService.getProperties(nodeRef)
        json.`object`()
        for (entry <- properties.entrySet()) {
            json.key(entry.getKey.toString).value(entry.getValue)
        }
        json.endObject()
        json
    }
}