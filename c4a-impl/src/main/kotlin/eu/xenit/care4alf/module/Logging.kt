package eu.xenit.care4alf.module

import com.github.dynamicextensionsalfresco.webscripts.annotations.*
import eu.xenit.care4alf.json
import org.apache.log4j.Level
import org.apache.log4j.LogManager
import org.apache.log4j.Logger
import org.json.JSONObject
import org.springframework.stereotype.Component
import java.util.*

/**
 * @author Laurent Van der Linden
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/logging", families = arrayOf("care4alf"), description = "Logger levels")
@Authentication(AuthenticationType.ADMIN)
public class Logging {
    @Uri()
    fun get(@RequestParam logger: String) = json {
        obj {
            entry("level", Logger.getLogger(logger).getLevel())
        }
    }

    @Uri(method = HttpMethod.POST)
    fun set(body: JSONObject) {

        val logger = if (body.has("logger")) body.getString("logger") else body.getString("name")
        val level = body.getString("level")

        Logger.getLogger(logger).setLevel(Level.toLevel(level))
    }

    @Uri("/all")
    fun loggers() = json {
        iterable(Collections.list(LogManager.getCurrentLoggers() as Enumeration<Logger>)) { logger ->
            obj {
                entry("name", logger.getName())
                entry("level", logger.getLevel())
            }
        }
    }
}