package xenit.care4alf.module

import org.springframework.stereotype.Component
import com.github.dynamicextensionsalfresco.webscripts.annotations._
import org.springframework.beans.factory.{DisposableBean, InitializingBean}
import xenit.care4alf.dumbster.smtp.{ServerOptions, SmtpServer, SmtpServerFactory}
import xenit.care4alf.web.{Json, JsonHelper}

import scala.collection.JavaConversions._

/**
 * @author Laurent Van der Linden
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/smtp", families = Array("care4alf"), description = "dummy SMTP viewer")
@Authentication(AuthenticationType.ADMIN)
class DummyMail extends InitializingBean with DisposableBean with Json {
    private var smtpServer: SmtpServer = null

    @Uri(Array("/list"))
    def listMails(@Attribute jsonHelper: JsonHelper) {
        val json = jsonHelper.json
        json.array()
        for (message <- smtpServer.getMessages) {
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

    def afterPropertiesSet() {
        val options = new ServerOptions()
        options.port = 2500
        smtpServer = SmtpServerFactory.startServer(options)
    }

    def destroy() {
        smtpServer.stop()
    }
}
