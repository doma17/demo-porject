package com.example.demoproject.analytics.application

import com.example.demoproject.analytics.persistence.ActivityLogRepository
import com.example.demoproject.analytics.persistence.ActivityType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.Instant

@Service
class AnalyticsService(
    private val activityLogRepository: ActivityLogRepository,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun getLastDayActivity(): ActivityStats {
        val until = Instant.now(clock)
        val since = until.minus(Duration.ofDays(1))
        return ActivityStats(
            since = since,
            until = until,
            signupCount = count(ActivityType.SIGNUP, since, until),
            loginCount = count(ActivityType.LOGIN, since, until),
            chatCreatedCount = count(ActivityType.CHAT_CREATED, since, until),
        )
    }

    private fun count(type: ActivityType, since: Instant, until: Instant): Long =
        activityLogRepository.countByTypeAndCreatedAtBetween(type, since, until)
}

data class ActivityStats(
    val since: Instant,
    val until: Instant,
    val signupCount: Long,
    val loginCount: Long,
    val chatCreatedCount: Long,
)
