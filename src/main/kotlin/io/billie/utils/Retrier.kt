package io.billie.utils

import org.slf4j.LoggerFactory

object Retrier {

    private val log = LoggerFactory.getLogger(this::class.java)
    private val MAX_ATTEMPTS = 5
    fun retry(messageOnFailedAttempt: String, failureMessage: String, action: () -> Unit) {
        var attempts = 0
        while (attempts < MAX_ATTEMPTS) {
            try {
                return action()
            } catch (e: Exception) {
                attempts++
                log.warn("$messageOnFailedAttempt. Attempt number <$attempts>. Will retry after 5 seconds")
                if (attempts == MAX_ATTEMPTS) {
                    log.error(failureMessage)
                    throw RuntimeException(failureMessage, e)
                }
                Thread.sleep(5000)
            }
        }
    }
}