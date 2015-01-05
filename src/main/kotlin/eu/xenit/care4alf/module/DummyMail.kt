package eu.xenit.care4alf.module

import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.DisposableBean
import eu.xenit.care4alf.json
import xenit.care4alf.dumbster.smtp.SmtpServer
import xenit.care4alf.dumbster.smtp.SmtpServerFactory
import org.slf4j.LoggerFactory
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri
import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript
import org.springframework.stereotype.Component
import xenit.care4alf.dumbster.smtp.ServerOptions
import com.github.dynamicextensionsalfresco.webscripts.annotations.HttpMethod

/**
 * @author Laurent Van der Linden
 */
Component
WebScript(baseUri = "/xenit/care4alf/smtp", families = array("care4alf"), description = "dummy SMTP viewer")
Authentication(AuthenticationType.ADMIN)
class DummyMail : InitializingBean, DisposableBean {
    val DEFAULT_PORT = 2500

    private var smtpServer: SmtpServer? = null

    private val logger = LoggerFactory.getLogger(javaClass)

    Uri(array("list"))
    fun list() = json {
        iterable(smtpServer!!.getMessages().toList()) { message ->
            obj {
                key("headers") {
                    obj {
                        for (headerName in message.getHeaderNames()) {
                            entry(headerName, message.getHeaderValues(headerName).join(","))
                        }
                    }
                }
            }
        }
    }

    Uri(value = array("/list"), method = HttpMethod.DELETE)
    fun clearMails() {
        smtpServer?.clearMessages()
    }

    Uri(array("/config"))
    fun config() = json {
        obj {
            entry("port", DEFAULT_PORT)
        }
    }

    override fun afterPropertiesSet() {
        val options = ServerOptions()
        options.port = DEFAULT_PORT
        try {
            smtpServer = SmtpServerFactory.startServer(options)
        }
        catch (ex: Exception){
            logger.warn("Failed to start SMTP server", ex)
        }
    }

    override fun destroy() {
        try {
            smtpServer?.stop()
        } catch(e: Exception) {
            logger.error("Failed to stop SMTP server")
        }
    }
}