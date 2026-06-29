package com.example.demoproject.feedback.application

import com.example.demoproject.common.error.DuplicateFeedbackException
import com.example.demoproject.common.error.ForbiddenOperationException
import com.example.demoproject.feedback.persistence.FeedbackEntity
import com.example.demoproject.feedback.persistence.FeedbackRepository
import com.example.demoproject.feedback.persistence.FeedbackStatus
import com.example.demoproject.user.persistence.UserEntity
import com.example.demoproject.user.persistence.UserRepository
import com.example.demoproject.user.persistence.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID

@Tag("unit")
class FeedbackServiceTest {
    private val feedbackRepository = mock(FeedbackRepository::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-06-29T00:00:00Z"), ZoneOffset.UTC)
    private val service = FeedbackService(feedbackRepository, userRepository, clock)

    @Test
    fun `create stores pending feedback once per user and chat`() {
        val user = user(id = UUID.randomUUID())
        val chatId = UUID.randomUUID()
        `when`(userRepository.findById(user.id!!)).thenReturn(Optional.of(user))
        `when`(feedbackRepository.existsByUser_IdAndChatId(user.id!!, chatId)).thenReturn(false)
        doAnswer { invocation ->
            (invocation.arguments[0] as FeedbackEntity).also { it.id = UUID.fromString("00000000-0000-0000-0000-000000000101") }
        }.`when`(feedbackRepository).save(any(FeedbackEntity::class.java))

        val result = service.create(CreateFeedbackCommand(user.id!!, chatId, positive = true))

        assertEquals(user.id, result.userId)
        assertEquals(chatId, result.chatId)
        assertEquals(true, result.positive)
        assertEquals(FeedbackStatus.pending, result.status)
        assertEquals(Instant.parse("2026-06-29T00:00:00Z"), result.createdAt)
    }

    @Test
    fun `create rejects duplicate feedback for same user and chat`() {
        val user = user(id = UUID.randomUUID())
        val chatId = UUID.randomUUID()
        `when`(userRepository.findById(user.id!!)).thenReturn(Optional.of(user))
        `when`(feedbackRepository.existsByUser_IdAndChatId(user.id!!, chatId)).thenReturn(true)

        assertThrows(DuplicateFeedbackException::class.java) {
            service.create(CreateFeedbackCommand(user.id!!, chatId, positive = false))
        }
    }

    @Test
    fun `member list is scoped to own feedback and positive filter`() {
        val user = user(id = UUID.randomUUID())
        val feedback = feedback(user = user, positive = false)
        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
        `when`(userRepository.findById(user.id!!)).thenReturn(Optional.of(user))
        `when`(feedbackRepository.findByUser_IdAndPositive(user.id!!, false, pageable)).thenReturn(PageImpl(listOf(feedback), pageable, 1))

        val result = service.list(FeedbackListQuery(user.id!!, positive = false, page = 0, size = 20, direction = Sort.Direction.DESC))

        assertEquals(1, result.totalElements)
        assertEquals(feedback.id, result.content.single().id)
    }

    @Test
    fun `member cannot update feedback status`() {
        val user = user(id = UUID.randomUUID(), role = UserRole.member)
        `when`(userRepository.findById(user.id!!)).thenReturn(Optional.of(user))

        assertThrows(ForbiddenOperationException::class.java) {
            service.updateStatus(UpdateFeedbackStatusCommand(user.id!!, UUID.randomUUID(), FeedbackStatus.resolved))
        }
    }

    @Test
    fun `admin updates feedback status`() {
        val admin = user(id = UUID.randomUUID(), role = UserRole.admin)
        val owner = user(id = UUID.randomUUID())
        val feedback = feedback(user = owner, status = FeedbackStatus.pending)
        `when`(userRepository.findById(admin.id!!)).thenReturn(Optional.of(admin))
        `when`(feedbackRepository.findById(feedback.id!!)).thenReturn(Optional.of(feedback))
        doAnswer { it.arguments[0] }.`when`(feedbackRepository).save(any(FeedbackEntity::class.java))

        val result = service.updateStatus(UpdateFeedbackStatusCommand(admin.id!!, feedback.id!!, FeedbackStatus.resolved))

        assertEquals(FeedbackStatus.resolved, result.status)
        verify(feedbackRepository).save(feedback)
    }

    private fun user(id: UUID, role: UserRole = UserRole.member): UserEntity = UserEntity(
        id = id,
        email = "$id@example.com",
        passwordHash = "hash",
        name = "User",
        role = role,
        createdAt = clock.instant(),
    )

    private fun feedback(
        user: UserEntity,
        positive: Boolean = true,
        status: FeedbackStatus = FeedbackStatus.pending,
    ): FeedbackEntity = FeedbackEntity(
        id = UUID.randomUUID(),
        user = user,
        chatId = UUID.randomUUID(),
        positive = positive,
        status = status,
        createdAt = clock.instant(),
    )
}
