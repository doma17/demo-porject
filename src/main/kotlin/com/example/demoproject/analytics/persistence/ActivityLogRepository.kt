package com.example.demoproject.analytics.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

interface ActivityLogRepository : JpaRepository<ActivityLogEntity, UUID> {
    fun countByTypeAndCreatedAtBetween(type: ActivityType, since: Instant, until: Instant): Long
}
