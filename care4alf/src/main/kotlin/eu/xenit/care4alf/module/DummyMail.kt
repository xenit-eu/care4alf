package eu.xenit.care4alf.module

import com.github.dynamicextensionsalfresco.webscripts.annotations.*
import eu.xenit.care4alf.json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Component
import eu.xenit.care4alf.dumbster.smtp.ServerOptions
import eu.xenit.care4alf.dumbster.smtp.SmtpServer
import eu.xenit.care4alf.dumbster.smtp.SmtpServerFactory

/**
 * @author Laurent Van der Linden
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/smtp", families = arrayOf("care4alf"), description = "dummy SMTP viewer")
@Authentication(AuthenticationType.ADMIN)
class DummyMail : InitializingBean, DisposableBean {
    val DEFAULT_PORT = 2500

    private var smtpServer: SmtpServer? = null

    private val logger = LoggerFactory.getLogger(javaClass)

    @Uri("list")
    fun list() = json {
        iterable(smtpServer!!.messages.toList()) { message ->
            obj {
                key("headers") {
                    obj {
                        for (headerName in message.getHeaderNames()) {
                            entry(headerName, message.getHeaderValues(headerName).joinToString(","))
                        }
                    }
                }
                entry("body", message.body)
            }
        }
    }

    @Uri(value = "/list", method = HttpMethod.DELETE)
    fun clearMails() {
        smtpServer?.clearMessages()
    }

    @Uri("/config")
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