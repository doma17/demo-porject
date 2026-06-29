package com.example.demoproject.analytics.application

import com.example.demoproject.analytics.persistence.ActivityLogEntity
import com.example.demoproject.analytics.persistence.ActivityLogRepository
import com.example.demoproject.analytics.persistence.ActivityType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

interface ActivityLogRecorder {
    fun recordSignup()
    fun recordLogin()
    fun recordChatCreated()
}

@Service
class ActivityLogService(
    private val activityLogRepository: ActivityLogRepository,
    private val clock: Clock,
) : ActivityLogRecorder {
    @Transactional
    override fun recordSignup() {
        record(ActivityType.SIGNUP)
    }

    @Transactional
    override fun recordLogin() {
        record(ActivityType.LOGIN)
    }

    @Transactional
    override fun recordChatCreated() {
        record(ActivityType.CHAT_CREATED)
    }

    private fun record(type: ActivityType) {
        activityLogRepository.save(ActivityLogEntity(type = type, createdAt = Instant.now(clock)))
    }
}
