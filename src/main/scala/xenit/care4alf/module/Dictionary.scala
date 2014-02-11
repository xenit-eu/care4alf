package xenit.care4alf.module

import org.springframework.beans.factory.annotation.Autowired
import org.alfresco.service.namespace.NamespaceService
import xenit.care4alf.web.{JsonHelper, Json}
import com.github.dynamicextensionsalfresco.webscripts.annotations.{WebScript, Uri, Attribute}

import scala.collection.JavaConversions._
import org.springframework.stereotype.Component

/**
 * @author Laurent Van der Linden
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/dictionary", families = Array("care4alf"), description = "Dictionary info")
class Dictionary @Autowired() (namespaceService: NamespaceService) extends Json
{
    @Uri(value = Array("namespaces"), defaultFormat = "json")
    def namespaces(@Attribute jsonHelper: JsonHelper) {
        val json = jsonHelper.json
        json.`object`()
        namespaceService.getURIs.filterNot(_.isEmpty).foreach(namespace => {
            json.key(namespace)
            json.array()
            namespaceService.getPrefixes(namespace).foreach(json.value(_))
            json.endArray()
        })
        json.endObject()
    }
}
