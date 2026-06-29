package com.example.demoproject.chat.application

import java.time.Duration
import java.time.Instant

class ThreadPolicy(
    private val reuseWindow: Duration = Duration.ofMinutes(30),
) {
    fun shouldReuse(lastChatAt: Instant, now: Instant): Boolean = !lastChatAt.isBefore(now.minus(reuseWindow))
}
