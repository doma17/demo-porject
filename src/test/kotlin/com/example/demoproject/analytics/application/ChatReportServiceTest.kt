package com.example.demoproject.analytics.application

import com.example.demoproject.analytics.persistence.ChatReportEntryEntity
import com.example.demoproject.analytics.persistence.ChatReportEntryRepository
import com.example.demoproject.user.persistence.UserEntity
import com.example.demoproject.user.persistence.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

@Tag("unit")
class ChatReportServiceTest {
    private val until = Instant.parse("2026-06-29T12:00:00Z")
    private val since = Instant.parse("2026-06-28T12:00:00Z")
    private val clock = Clock.fixed(until, ZoneOffset.UTC)
    private val repository = mock(ChatReportEntryRepository::class.java)
    private val recorder = RecordingActivityLogRecorder()
    private val service = ChatReportService(repository, recorder, clock)

    @Test
    fun `record chat stores report entry and increments activity`() {
        val user = user()
        `when`(repository.save(org.mockito.ArgumentMatchers.any(ChatReportEntryEntity::class.java))).thenAnswer { it.arguments[0] }

        service.recordChat(user, "question", "answer")

        val captor = ArgumentCaptor.forClass(ChatReportEntryEntity::class.java)
        verify(repository).save(captor.capture())
        assertEquals(user, captor.value.user)
        assertEquals("question", captor.value.question)
        assertEquals("answer", captor.value.answer)
        assertEquals(until, captor.value.createdAt)
        assertEquals(1, recorder.chatCreatedCount)
    }

    @Test
    fun `csv report includes last day chat rows with escaping`() {
        `when`(repository.findByCreatedAtBetweenOrderByCreatedAtAsc(since, until)).thenReturn(
            listOf(
                ChatReportEntryEntity(
                    user = user(email = "member@example.com", name = "Member"),
                    question = "hello, bot",
                    answer = "hi \"member\"",
                    createdAt = Instant.parse("2026-06-29T00:00:00Z"),
                ),
            ),
        )

        val csv = service.generateLastDayCsv()

        assertTrue(csv.startsWith("createdAt,userId,email,name,question,answer\n"))
        assertTrue(csv.contains("member@example.com"))
        assertTrue(csv.contains("\"hello, bot\""))
        assertTrue(csv.contains("\"hi \"\"member\"\"\""))
        verify(repository).findByCreatedAtBetweenOrderByCreatedAtAsc(since, until)
    }

    private fun user(
        email: String = "member@example.com",
        name: String = "Member",
    ): UserEntity = UserEntity(
        id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
        email = email,
        passwordHash = "hash",
        name = name,
        role = UserRole.member,
    )

    private class RecordingActivityLogRecorder : ActivityLogRecorder {
        var chatCreatedCount = 0

        override fun recordSignup() = Unit

        override fun recordLogin() = Unit

        override fun recordChatCreated() {
            chatCreatedCount += 1
        }
    }
}
