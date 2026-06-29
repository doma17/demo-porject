package com.example.demoproject.feedback.application

import com.example.demoproject.common.error.DuplicateFeedbackException
import com.example.demoproject.common.error.FeedbackNotFoundException
import com.example.demoproject.common.error.ForbiddenOperationException
import com.example.demoproject.common.error.InvalidCredentialsException
import com.example.demoproject.feedback.persistence.FeedbackEntity
import com.example.demoproject.feedback.persistence.FeedbackRepository
import com.example.demoproject.feedback.persistence.FeedbackStatus
import com.example.demoproject.user.persistence.UserEntity
import com.example.demoproject.user.persistence.UserRepository
import com.example.demoproject.user.persistence.UserRole
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class FeedbackService(
    private val feedbackRepository: FeedbackRepository,
    private val userRepository: UserRepository,
    private val clock: Clock,
) {
    @Transactional
    fun create(command: CreateFeedbackCommand): FeedbackResult {
        val user = findUser(command.userId)
        val userId = requireNotNull(user.id)
        if (feedbackRepository.existsByUser_IdAndChatId(userId, command.chatId)) throw DuplicateFeedbackException()

        val feedback = feedbackRepository.save(
            FeedbackEntity(
                user = user,
                chatId = command.chatId,
                positive = command.positive,
                status = FeedbackStatus.pending,
                createdAt = Instant.now(clock),
            ),
        )
        return feedback.toResult()
    }

    @Transactional(readOnly = true)
    fun list(query: FeedbackListQuery): Page<FeedbackResult> {
        val user = findUser(query.userId)
        val userId = requireNotNull(user.id)
        val pageable = PageRequest.of(query.page, query.size, Sort.by(query.direction, "createdAt"))
        val feedback = when {
            user.role == UserRole.admin && query.positive != null -> feedbackRepository.findByPositive(query.positive, pageable)
            user.role == UserRole.admin -> feedbackRepository.findAll(pageable)
            query.positive != null -> feedbackRepository.findByUser_IdAndPositive(userId, query.positive, pageable)
            else -> feedbackRepository.findByUser_Id(userId, pageable)
        }
        return feedback.map { it.toResult() }
    }

    @Transactional
    fun updateStatus(command: UpdateFeedbackStatusCommand): FeedbackResult {
        val user = findUser(command.userId)
        if (user.role != UserRole.admin) throw ForbiddenOperationException("Admin role is required")

        val feedback = feedbackRepository.findById(command.feedbackId).orElseThrow { FeedbackNotFoundException() }
        feedback.resolve(command.status)
        return feedbackRepository.save(feedback).toResult()
    }

    private fun findUser(userId: UUID): UserEntity =
        userRepository.findById(userId).orElseThrow { InvalidCredentialsException() }

    private fun FeedbackEntity.toResult(): FeedbackResult = FeedbackResult(
        id = requireNotNull(id),
        userId = requireNotNull(user.id),
        chatId = chatId,
        positive = positive,
        status = status,
        createdAt = createdAt,
    )
}

data class CreateFeedbackCommand(val userId: UUID, val chatId: UUID, val positive: Boolean)

data class FeedbackListQuery(
    val userId: UUID,
    val positive: Boolean?,
    val page: Int,
    val size: Int,
    val direction: Sort.Direction,
)

data class UpdateFeedbackStatusCommand(val userId: UUID, val feedbackId: UUID, val status: FeedbackStatus)

data class FeedbackResult(
    val id: UUID,
    val userId: UUID,
    val chatId: UUID,
    val positive: Boolean,
    val status: FeedbackStatus,
    val createdAt: Instant,
)
