package xenit.care4alf.module

import org.springframework.stereotype.Component
import com.github.dynamicextensionsalfresco.webscripts.annotations._
import org.springframework.beans.factory.{DisposableBean, InitializingBean}
import xenit.care4alf.dumbster.smtp.{ServerOptions, SmtpServer, SmtpServerFactory}
import xenit.care4alf.web.{Json, JsonHelper}

import scala.collection.JavaConversions._
import com.typesafe.scalalogging.slf4j.Logging

/**
 * @author Laurent Van der Linden
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/smtp", families = Array("care4alf"), description = "dummy SMTP viewer")
@Authentication(AuthenticationType.ADMIN)
class DummyMail extends InitializingBean with DisposableBean with Json with Logging {
    private var smtpServer: SmtpServer = null

    @Uri(Array("/list"))
    def listMails(@Attribute jsonHelper: JsonHelper) {
        val json = jsonHelper.json
        json.array()
        for (message <- smtpServer.getMessages.reverse) {
            json.`object`()
                .key("headers").`object`()
            for (headerName <- message.getHeaderNames) {
                json.key(headerName).value(message.getHeaderValues(headerName).mkString(","))
            }
            json.endObject()
            json.key("body").value(message.getBody)
            json.endObject()
        }
        json.endArray()
    }

    @Uri(value = Array("/list"), method = HttpMethod.DELETE)
    def clearMails() {
        smtpServer.clearMessages()
    }

    def afterPropertiesSet() {
        val options = new ServerOptions()
        options.port = 2500
        try {
            smtpServer = SmtpServerFactory.startServer(options)
        }
        catch {
            case ex: Exception => logger.warn("Failed to start SMTP server", ex)
        }
    }

    def destroy() {
        if (smtpServer != null) {
            try {
                smtpServer.stop()
            }
            catch {
                case ex: Exception => logger.warn("Failed to stop SMTP server", ex)
            }
        }
    }
}
