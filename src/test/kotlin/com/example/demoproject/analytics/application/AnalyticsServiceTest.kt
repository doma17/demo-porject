package com.example.demoproject.analytics.application

import com.example.demoproject.analytics.persistence.ActivityLogRepository
import com.example.demoproject.analytics.persistence.ActivityType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@Tag("unit")
class AnalyticsServiceTest {
    private val until = Instant.parse("2026-06-29T12:00:00Z")
    private val since = Instant.parse("2026-06-28T12:00:00Z")
    private val clock = Clock.fixed(until, ZoneOffset.UTC)
    private val repository = mock(ActivityLogRepository::class.java)
    private val service = AnalyticsService(repository, clock)

    @Test
    fun `counts activity for the last day`() {
        `when`(repository.countByTypeAndCreatedAtBetween(ActivityType.SIGNUP, since, until)).thenReturn(2)
        `when`(repository.countByTypeAndCreatedAtBetween(ActivityType.LOGIN, since, until)).thenReturn(3)
        `when`(repository.countByTypeAndCreatedAtBetween(ActivityType.CHAT_CREATED, since, until)).thenReturn(4)

        val result = service.getLastDayActivity()

        assertEquals(since, result.since)
        assertEquals(until, result.until)
        assertEquals(2, result.signupCount)
        assertEquals(3, result.loginCount)
        assertEquals(4, result.chatCreatedCount)
        verify(repository).countByTypeAndCreatedAtBetween(ActivityType.SIGNUP, since, until)
        verify(repository).countByTypeAndCreatedAtBetween(ActivityType.LOGIN, since, until)
        verify(repository).countByTypeAndCreatedAtBetween(ActivityType.CHAT_CREATED, since, until)
    }
}
