package eu.xenit.care4alf.web

import org.slf4j.Logger


abstract class LogHelper {
    abstract val logger: Logger

    inline fun debug(message: () -> String) {
        if (logger.isDebugEnabled()) {
            logger.debug(message())
        }
    }

    inline fun info(message: () -> String) {
        if (logger.isInfoEnabled()) {
            logger.info(message())
        }
    }

    fun error(message: String, throwable: Throwable? = null) {
        if (logger.isErrorEnabled()) {
            logger.error(message, throwable)
        }
    }
}