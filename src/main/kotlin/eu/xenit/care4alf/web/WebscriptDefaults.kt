package eu.xenit.care4alf.web

import com.github.dynamicextensionsalfresco.webscripts.annotations.ExceptionHandler
import eu.xenit.care4alf

interface WebscriptDefaults {
    ExceptionHandler(Throwable::class)
    fun exceptionHandler(exception: Throwable) = care4alf.json {
        obj {
            entry("message", exception.getMessage() ?: "NullPointer")
        }
    }
}