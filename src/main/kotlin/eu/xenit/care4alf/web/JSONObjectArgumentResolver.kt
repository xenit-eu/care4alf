package eu.xenit.care4alf.web

import com.github.dynamicextensionsalfresco.webscripts.arguments.AbstractTypeBasedArgumentResolver
import org.json.JSONObject
import org.springframework.extensions.webscripts.WebScriptRequest
import org.springframework.extensions.webscripts.WebScriptResponse
import org.springframework.stereotype.Component

/**
 * Created by laurentvdl on 11/14/14.
 */
@Component
public class JSONObjectArgumentResolver : AbstractTypeBasedArgumentResolver<JSONObject>() {
    override fun getExpectedArgumentType() = JSONObject::class.java

    override fun resolveArgument(request: WebScriptRequest, response: WebScriptResponse): JSONObject? {
        val content = request.getContent().getContent()
        if ("" == content) return null
        try {
            return JSONObject(content)
        }
        catch(exception: Exception) {
            return null
        }
    }
}