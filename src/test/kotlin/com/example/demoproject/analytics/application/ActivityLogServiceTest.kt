package com.example.demoproject.analytics.application

import com.example.demoproject.analytics.persistence.ActivityLogEntity
import com.example.demoproject.analytics.persistence.ActivityLogRepository
import com.example.demoproject.analytics.persistence.ActivityType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@Tag("unit")
class ActivityLogServiceTest {
    private val now = Instant.parse("2026-06-29T00:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val repository = mock(ActivityLogRepository::class.java)
    private val service = ActivityLogService(repository, clock)

    @Test
    fun `records signup activity with current timestamp`() {
        service.recordSignup()

        val captor = ArgumentCaptor.forClass(ActivityLogEntity::class.java)
        verify(repository).save(captor.capture())
        assertEquals(ActivityType.SIGNUP, captor.value.type)
        assertEquals(now, captor.value.createdAt)
    }
}
