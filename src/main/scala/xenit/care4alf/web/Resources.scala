package xenit.care4alf.web

import com.github.dynamicextensionsalfresco.webscripts.annotations._
import org.springframework.stereotype.Component
import java.lang.String
import org.springframework.extensions.webscripts.WebScriptResponse
import com.github.dynamicextensionsalfresco.webscripts.support.AbstractBundleResourceHandler

/**
 * @author Laurent Van der Linden
 */
@Component
@WebScript
@Authentication (AuthenticationType.NONE)
class Resources extends AbstractBundleResourceHandler {
    private[this] val packagePath = this.getClass.getPackage.getName.replace('.', '/')

    @Uri(value = Array("/xenit/care4alf/resources/{path}"), formatStyle = FormatStyle.ARGUMENT)
    def handleResources(@UriVariable path: String, response: WebScriptResponse) {
        checkCacheOptions(path, response)
    }

    @Uri(value = Array("/xenit/care4alf/cached/{version}/{path}"), formatStyle = FormatStyle.ARGUMENT)
    def handleVersionedResources(@UriVariable version: Long, @UriVariable path: String, response: WebScriptResponse) {
        if (version > 1) setInfinateCache(response)
        checkCacheOptions(path, response)
    }

    private def checkCacheOptions(path: String, response: WebScriptResponse) {
        if (path.indexOf(".cached.") > 0) {
            setInfinateCache(response)
        }
        handleResource(path, response)
    }

    protected override def getBundleEntryPath(path: String) = String.format("%s/%s", packagePath, path)
}
