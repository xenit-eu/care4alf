package eu.xenit.care4alf.web

import com.github.dynamicextensionsalfresco.webscripts.annotations.ExceptionHandler

interface WebscriptDefaults {
    @ExceptionHandler(Throwable::class)
    fun exceptionHandler(exception: Throwable) = eu.xenit.care4alf.json {
        obj {
            entry("message", exception.message ?: "NullPointer")
        }
    }
}