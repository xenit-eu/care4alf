package xenit.care4alf.module

import org.springframework.beans.factory.annotation.Autowired
import org.alfresco.service.namespace.NamespaceService
import xenit.care4alf.web.{JsonHelper, Json}
import com.github.dynamicextensionsalfresco.webscripts.annotations._

import scala.collection.JavaConversions._
import org.springframework.stereotype.Component
import org.alfresco.repo.i18n.{MessageServiceImpl, MessageService}
import org.springframework.extensions.webscripts.WebScriptResponse
import com.typesafe.scalalogging.slf4j.Logging

/**
 * @author Laurent Van der Linden
 */
//@Component
@WebScript(baseUri = "/xenit/care4alf/messages", families = Array("care4alf"), description = "Message service diagnostic")
@Authentication(AuthenticationType.ADMIN)
class Messages @Autowired() (namespaceService: NamespaceService) extends Json with Logging
{
    @Autowired var messageService: MessageService = null

    @Uri(defaultFormat = "json", method = HttpMethod.GET)
    def list(@Attribute jsonHelper: JsonHelper) {
        val json = jsonHelper.json
        json.array()
        for (bundle <- messageService.asInstanceOf[MessageServiceImpl].getRegisteredBundles) {
            json.value(bundle)
        }
        json.endArray()
    }

    @Uri(value = Array("/{key}"), defaultFormat = "json", method = HttpMethod.GET)
    def getMessage(@UriVariable key: String, response: WebScriptResponse) {
        response.getWriter.append(messageService.getMessage(key));
    }
}
