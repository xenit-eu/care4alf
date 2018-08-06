package eu.xenit.care4alf.web

import com.github.dynamicextensionsalfresco.webscripts.annotations.ExceptionHandler
import org.json.JSONWriter
import org.slf4j.Logger
import org.springframework.extensions.webscripts.WebScriptResponse
import javax.servlet.http.HttpServletResponse

/**
 * @author Laurent Van der Linden
 */
public interface RestErrorHandling {
    var logger: Logger

    @ExceptionHandler(Exception::class)
    fun handleIllegalArgument(exception: Exception, response: WebScriptResponse) {
        logger.error("Controller failure", exception)

        var cause: Throwable = exception
        while (cause.cause != null) {
            cause = cause.cause!!
        }

        if (cause is IllegalArgumentException) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
        }
        JSONWriter(response.getWriter()).`object`()
            .key("message").value(cause.message)
        .endObject()
    }
}