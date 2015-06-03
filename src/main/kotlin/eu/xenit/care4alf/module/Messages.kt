package eu.xenit.care4alf.module

import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType
import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import org.alfresco.repo.i18n.MessageService
import eu.xenit.care4alf.json
import org.alfresco.repo.i18n.MessageServiceImpl
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri
import com.github.dynamicextensionsalfresco.webscripts.annotations.HttpMethod
import com.github.dynamicextensionsalfresco.annotations.AlfrescoService
import com.github.dynamicextensionsalfresco.annotations.ServiceType
import org.slf4j.LoggerFactory
import eu.xenit.care4alf.web.LogHelper
import org.json.JSONObject
import java.util.*

/**
 * @author Laurent Van der Linden
 */
Component
WebScript(baseUri = "/xenit/care4alf/messages", families = arrayOf("care4alf"), description = "Message service diagnostic")
Authentication(AuthenticationType.ADMIN)
class Messages () : LogHelper {
    override val logger = LoggerFactory.getLogger(javaClass)

    Autowired AlfrescoService(ServiceType.LOW_LEVEL) var messageService: MessageService? = null

    Uri(value = "/bundles")
    fun list() = json {
        if (messageService is MessageServiceImpl) {
            iterable((messageService as MessageServiceImpl).getRegisteredBundles()) { bundle ->
                value(bundle)
            }
        } else {
            logger.error("MessageService is not of expected type (MessageServiceImpl), but ${messageService?.javaClass}")
        }
    }

    Uri("translate", method = HttpMethod.POST)
    fun translate(body: JSONObject) = json {
        val key = body.getString("key")
        iterable(Locale.getAvailableLocales().toArrayList()) { locale ->
            obj {
                entry(locale.toString(), messageService!!.getMessage(key, locale) ?: "-")
            }
        }
    }
}
