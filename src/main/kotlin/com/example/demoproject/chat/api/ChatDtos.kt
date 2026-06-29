package com.example.demoproject.chat.api

import com.example.demoproject.chat.application.ChatResult
import com.example.demoproject.chat.application.ThreadPageResult
import com.example.demoproject.chat.application.ThreadResult
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateChatRequest(
    @field:NotBlank
    @field:Size(max = 8_000)
    val question: String,
    val isStreaming: Boolean = false,
    val model: String? = null,
)

data class ChatResponse(
    val threadId: UUID,
    val chatId: UUID,
    val userId: UUID,
    val question: String,
    val answer: String,
    val model: String?,
    val createdAt: Instant,
)

data class ThreadResponse(
    val id: UUID,
    val userId: UUID,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastChatAt: Instant,
    val chats: List<ChatResponse>,
)

data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

fun ChatResult.toResponse(): ChatResponse = ChatResponse(threadId, chatId, userId, question, answer, model, createdAt)

fun ThreadResult.toResponse(): ThreadResponse = ThreadResponse(id, userId, createdAt, updatedAt, lastChatAt, chats.map { it.toResponse() })

fun ThreadPageResult.toResponse(): PageResponse<ThreadResponse> = PageResponse(
    content = content.map { it.toResponse() },
    page = page,
    size = size,
    totalElements = totalElements,
    totalPages = totalPages,
)
