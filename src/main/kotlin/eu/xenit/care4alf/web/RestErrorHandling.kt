package eu.xenit.care4alf.web

import org.springframework.extensions.webscripts.WebScriptResponse
import org.slf4j.Logger
import com.github.dynamicextensionsalfresco.webscripts.annotations.ExceptionHandler
import org.json.JSONWriter

/**
 * @author Laurent Van der Linden
 */
public trait RestErrorHandling {
    var logger: Logger

    ExceptionHandler(javaClass<Exception>())
    fun handleIllegalArgument(exception: Exception, response: WebScriptResponse) {
        logger.error("Controller failure", exception)
        if (exception is IllegalArgumentException) {
            response.setStatus(404)
        } else {
            response.setStatus(500)
        }
        JSONWriter(response.getWriter()).`object`()
            .key("message").value(exception.getMessage())
        .endObject()
    }
}