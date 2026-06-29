package com.example.demoproject.chat.application

import com.example.demoproject.ai.AiClient
import com.example.demoproject.ai.AiCompletionCommand
import com.example.demoproject.ai.AiMessage
import com.example.demoproject.chat.persistence.ChatEntity
import com.example.demoproject.chat.persistence.ChatRepository
import com.example.demoproject.chat.persistence.ChatThreadEntity
import com.example.demoproject.chat.persistence.ChatThreadRepository
import com.example.demoproject.common.error.ForbiddenException
import com.example.demoproject.common.error.StreamingNotReadyException
import com.example.demoproject.common.error.ThreadNotFoundException
import com.example.demoproject.user.persistence.UserEntity
import com.example.demoproject.user.persistence.UserRepository
import com.example.demoproject.user.persistence.UserRole
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class ChatService(
    private val userRepository: UserRepository,
    private val threadRepository: ChatThreadRepository,
    private val chatRepository: ChatRepository,
    private val aiClient: AiClient,
    private val clock: Clock,
) {
    private val threadPolicy = ThreadPolicy()

    @Transactional
    fun createChat(command: CreateChatCommand): ChatResult {
        if (command.isStreaming) throw StreamingNotReadyException()

        val user = findUser(command.userId)
        val now = Instant.now(clock)
        val thread = findReusableThread(user, now) ?: threadRepository.save(
            ChatThreadEntity(user = user, createdAt = now, updatedAt = now, lastChatAt = now),
        )
        val history = chatRepository.findByThreadIdOrderByCreatedAtAsc(requireNotNull(thread.id))
        val aiResult = aiClient.complete(
            AiCompletionCommand(
                model = command.model,
                messages = buildMessages(history, command.question),
            ),
        )
        val chat = chatRepository.save(
            ChatEntity(
                thread = thread,
                user = user,
                question = command.question.trim(),
                answer = aiResult.answer,
                model = aiResult.model,
                createdAt = now,
            ),
        )
        thread.touch(now)
        threadRepository.save(thread)
        return chat.toResult(requireNotNull(thread.id))
    }

    @Transactional(readOnly = true)
    fun listThreads(query: ListThreadsQuery): ThreadPageResult {
        val currentUser = findUser(query.userId)
        val pageable = query.toPageable()
        val page = if (currentUser.role == UserRole.admin) {
            threadRepository.findAllActive(pageable)
        } else {
            threadRepository.findActiveByUserId(query.userId, pageable)
        }
        val threadIds = page.content.mapNotNull { it.id }
        val chatsByThread = if (threadIds.isEmpty()) {
            emptyMap()
        } else {
            chatRepository.findByThreadIdsOrderByCreatedAtAsc(threadIds).groupBy { requireNotNull(it.thread.id) }
        }
        return ThreadPageResult(
            content = page.content.map { thread ->
                ThreadResult(
                    id = requireNotNull(thread.id),
                    userId = requireNotNull(thread.user.id),
                    createdAt = thread.createdAt,
                    updatedAt = thread.updatedAt,
                    lastChatAt = thread.lastChatAt,
                    chats = chatsByThread[requireNotNull(thread.id)].orEmpty().map { it.toResult(requireNotNull(thread.id)) },
                )
            },
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }

    @Transactional
    fun deleteThread(command: DeleteThreadCommand) {
        val currentUser = findUser(command.userId)
        val thread = threadRepository.findActiveById(command.threadId).orElseThrow { ThreadNotFoundException() }
        val ownerId = requireNotNull(thread.user.id)
        if (currentUser.role != UserRole.admin && ownerId != command.userId) throw ForbiddenException()
        thread.softDelete(Instant.now(clock))
        threadRepository.save(thread)
    }

    private fun findReusableThread(user: UserEntity, now: Instant): ChatThreadEntity? {
        val userId = requireNotNull(user.id)
        return threadRepository.findLatestActiveByUserId(userId, PageRequest.of(0, 1)).firstOrNull()
            ?.takeIf { threadPolicy.shouldReuse(it.lastChatAt, now) }
    }

    private fun buildMessages(history: List<ChatEntity>, question: String): List<AiMessage> {
        val messages = mutableListOf(
            AiMessage("system", "You are a concise, helpful customer-demo chatbot. Answer in the user's language when possible."),
        )
        history.forEach { chat ->
            messages += AiMessage("user", chat.question)
            messages += AiMessage("assistant", chat.answer)
        }
        messages += AiMessage("user", question.trim())
        return messages
    }

    private fun findUser(userId: UUID): UserEntity = userRepository.findById(userId).orElseThrow { ForbiddenException() }

    private fun ListThreadsQuery.toPageable(): Pageable {
        val direction = if (sortDirection.equals("asc", ignoreCase = true)) Sort.Direction.ASC else Sort.Direction.DESC
        val property = when (sortProperty) {
            "createdAt" -> "createdAt"
            "lastChatAt" -> "lastChatAt"
            "updatedAt" -> "updatedAt"
            else -> "lastChatAt"
        }
        return PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100), Sort.by(direction, property))
    }

    private fun ChatEntity.toResult(threadId: UUID): ChatResult = ChatResult(
        threadId = threadId,
        chatId = requireNotNull(id),
        userId = requireNotNull(user.id),
        question = question,
        answer = answer,
        model = model,
        createdAt = createdAt,
    )
}

data class CreateChatCommand(
    val userId: UUID,
    val question: String,
    val isStreaming: Boolean,
    val model: String?,
)

data class ListThreadsQuery(
    val userId: UUID,
    val page: Int,
    val size: Int,
    val sortProperty: String,
    val sortDirection: String,
)

data class DeleteThreadCommand(
    val userId: UUID,
    val threadId: UUID,
)

data class ChatResult(
    val threadId: UUID,
    val chatId: UUID,
    val userId: UUID,
    val question: String,
    val answer: String,
    val model: String?,
    val createdAt: Instant,
)

data class ThreadResult(
    val id: UUID,
    val userId: UUID,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastChatAt: Instant,
    val chats: List<ChatResult>,
)

data class ThreadPageResult(
    val content: List<ThreadResult>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
