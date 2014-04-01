package xenit.care4alf.web

import org.json.JSONObject
import com.github.dynamicextensionsalfresco.webscripts.arguments.AbstractTypeBasedArgumentResolver
import org.springframework.extensions.webscripts.{WebScriptResponse, WebScriptRequest}
import org.springframework.stereotype.Component

/**
 * Provide JsonObject argument resolution, requires DE 1.0.0
 *
 * @author Laurent Van der Linden
 */
@Component
class JsonObjectArgumentResolver extends AbstractTypeBasedArgumentResolver[JSONObject] {
    override def resolveArgument(request: WebScriptRequest, response: WebScriptResponse): JSONObject = {
        val content = request.getContent().getContent()
        if ("" == content) return null
        try {
            new JSONObject(content)
        }
        catch {
            case exception: Exception => null
        }
    }

    override def getExpectedArgumentType: Class[_] = classOf[JSONObject]
}
