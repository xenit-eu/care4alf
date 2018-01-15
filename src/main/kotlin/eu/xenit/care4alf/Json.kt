package eu.xenit.care4alf

import com.github.dynamicextensionsalfresco.webscripts.resolutions.JsonWriterResolution
import com.github.dynamicextensionsalfresco.webscripts.resolutions.Resolution
import org.json.JSONWriter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

/**
 * JSON builder to safely construct JSON hierarchies
 *
 * @author Laurent Van der Linden
 */
open class JsonRoot(val jsonWriter: JSONWriter) {

    inline fun obj(body: JsonObject.() -> Unit) {
        jsonWriter.`object`()
        JsonObject(jsonWriter).body()
        jsonWriter.endObject()
    }

    inline fun <T> iterable(iterable: Iterable<T>?, toJson: JsonRoot.(T) -> Unit) {
        jsonWriter.array()
        if (iterable != null) {
            for (item in iterable) {
                if (item != null) {
                    JsonRoot(jsonWriter).toJson(item)
                }
            }
        }
        jsonWriter.endArray()
    }

    inline fun <T> enumeration(enumeration: Enumeration<T>?, toJson: JsonRoot.(T) -> Unit) {
        jsonWriter.array()
        if (enumeration != null) {
            for (item in enumeration) {
                if (item != null) {
                    JsonRoot(jsonWriter).toJson(item)
                }
            }
        }
        jsonWriter.endArray()
    }

    fun value(value: Any) {
        jsonWriter.value(value)
    }
}

class JsonObject(val jsonWriter: JSONWriter) {
    fun entry(key: String, value: Any?) {
        if (value != null) {
            jsonWriter.key(key).value(value)
        }
    }
    inline fun key(key: String, body: JsonRoot.() -> Unit) {
        jsonWriter.key(key)
        JsonRoot(jsonWriter).body()
    }
}

class JsonArray(val jsonWriter: JSONWriter) {
    inline fun obj(body: JsonObject.() -> Unit) {
        jsonWriter.`object`()
        JsonObject(jsonWriter).body()
        jsonWriter.endObject()
    }
}

fun json(body: JsonRoot.() -> Unit): Resolution {
    return object : JsonWriterResolution() {
        override fun writeJson(jsonWriter: JSONWriter?) {
            JsonRoot(jsonWriter!!).body()
        }
    }
}