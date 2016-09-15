package eu.xenit.care4alf.web

import com.github.dynamicextensionsalfresco.webscripts.annotations.*
import com.github.dynamicextensionsalfresco.webscripts.support.AbstractBundleResourceHandler
import org.springframework.extensions.webscripts.WebScriptResponse
import org.springframework.stereotype.Component

/**
 * @author Laurent Van der Linden
 */
@Component
@WebScript(families = arrayOf("care4alf"))
@Authentication(AuthenticationType.NONE)
@Transaction(readOnly = true)
public class Resources : AbstractBundleResourceHandler() {
    private val packagePath = this.javaClass.getPackage().getName().replace('.', '/')

    @Uri(value = "/xenit/care4alf/resources/{path}", formatStyle = FormatStyle.ARGUMENT)
    fun handleResources(@UriVariable path: String, response: WebScriptResponse) {
        checkCacheOptions(path, response)
    }

    @Uri(value = "/xenit/care4alf/cached/{version}/{path}", formatStyle = FormatStyle.ARGUMENT)
    fun handleVersionedResources(@UriVariable version: Long, @UriVariable path: String, response: WebScriptResponse) {
        if (version > 1) setInfinateCache(response)
        checkCacheOptions(path, response)
    }

    private fun checkCacheOptions(path: String, response: WebScriptResponse) {
        if (path.indexOf(".cached.") > 0) {
            setInfinateCache(response)
        }
        handleResource(path, response)
    }

    protected override fun getBundleEntryPath(path: String) = "$packagePath/$path"
}