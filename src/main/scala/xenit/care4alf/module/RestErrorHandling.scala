package xenit.care4alf.module

import com.github.dynamicextensionsalfresco.webscripts.annotations.ExceptionHandler
import org.springframework.extensions.webscripts.WebScriptResponse
import com.typesafe.scalalogging.slf4j.Logging

/**
 * @author Laurent Van der Linden
 */
trait RestErrorHandling extends Logging {
    @ExceptionHandler(Array(classOf[IllegalArgumentException]))
    def handleIllegalArgument(exception: Exception, response: WebScriptResponse) {
        logger.error("Illegal argument", exception)
        response.setStatus(404)
        response.getWriter.write(exception.getMessage)
    }
}
