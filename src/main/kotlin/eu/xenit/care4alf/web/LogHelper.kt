package eu.xenit.care4alf.web

import org.slf4j.Logger

interface LogHelper {
    protected val logger: Logger

    inline final fun debug(message: () -> String) {
        if (logger.isDebugEnabled()) {
            logger.debug(message())
        }
    }

    inline final fun info(message: () -> String) {
        if (logger.isInfoEnabled()) {
            logger.info(message())
        }
    }

    final fun error(message: String, throwable: Throwable? = null) {
        if (logger.isErrorEnabled()) {
            logger.error(message, throwable)
        }
    }
}