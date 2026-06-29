package com.example.demoproject.analytics.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

interface ChatReportEntryRepository : JpaRepository<ChatReportEntryEntity, UUID> {
    fun findByCreatedAtBetweenOrderByCreatedAtAsc(since: Instant, until: Instant): List<ChatReportEntryEntity>
}
