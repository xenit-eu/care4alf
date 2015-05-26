package eu.xenit.care4alf.web

import org.springframework.extensions.webscripts.WebScriptResponse
import org.slf4j.Logger
import com.github.dynamicextensionsalfresco.webscripts.annotations.ExceptionHandler
import org.json.JSONWriter
import javax.servlet.http.HttpServletResponse

/**
 * @author Laurent Van der Linden
 */
public trait RestErrorHandling {
    var logger: Logger

    ExceptionHandler(javaClass<Exception>())
    fun handleIllegalArgument(exception: Exception, response: WebScriptResponse) {
        logger.error("Controller failure", exception)

        var cause: Throwable = exception
        while (cause.getCause() != null) {
            cause = cause.getCause()!!
        }

        if (cause is IllegalArgumentException) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
        }
        JSONWriter(response.getWriter()).`object`()
            .key("message").value(cause.getMessage())
        .endObject()
    }
}