package xenit.care4alf.web

import com.github.dynamicextensionsalfresco.webscripts.annotations.{Attribute, Before}
import java.lang.{Long, String}
import scala.Predef.String
import org.eclipse.gemini.blueprint.context.BundleContextAware
import org.osgi.framework.BundleContext
import java.util

/**
 * @author Laurent Van der Linden
 */
trait Versioned extends BundleContextAware {
    var bundleContext: BundleContext = null

    @Before def setResourceVersion(model: util.Map[String,Object]) {
        val lastModified = bundleContext.getBundle.getHeaders.get("Bnd-LastModified")

        if (lastModified != null) {
            model.put("version", new Long(lastModified))
        }
    }

    def setBundleContext(bundleContext: BundleContext) {
        this.bundleContext = bundleContext
    }
}
