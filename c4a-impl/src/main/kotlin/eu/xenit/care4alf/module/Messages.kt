package eu.xenit.care4alf.module

import com.github.dynamicextensionsalfresco.annotations.AlfrescoService
import com.github.dynamicextensionsalfresco.annotations.ServiceType
import com.github.dynamicextensionsalfresco.webscripts.annotations.*
import eu.xenit.care4alf.json
import eu.xenit.care4alf.web.LogHelper
import org.alfresco.repo.i18n.MessageService
import org.alfresco.repo.i18n.MessageServiceImpl
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*

/**
 * @author Laurent Van der Linden
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/messages", families = arrayOf("care4alf"), description = "Message service diagnostic")
@Authentication(AuthenticationType.ADMIN)
class Messages () : LogHelper() {
    override val logger = LoggerFactory.getLogger(javaClass)

    @Autowired @AlfrescoService(ServiceType.LOW_LEVEL) var messageService: MessageService? = null

    @Uri("/bundles")
    fun list() = json {
        if (messageService is MessageServiceImpl) {
            iterable((messageService as MessageServiceImpl).getRegisteredBundles()) { bundle ->
                value(bundle)
            }
        } else {
            logger.error("MessageService is not of expected type (MessageServiceImpl), but ${messageService?.javaClass}")
        }
    }

    @Uri("translate", method = HttpMethod.POST)
    fun translate(body: JSONObject) = json {
        val key = body.getString("key")
        iterable(Locale.getAvailableLocales().asList()) { locale ->
            obj {
                entry(locale.toString(), messageService!!.getMessage(key, locale) ?: "-")
            }
        }
    }
}
