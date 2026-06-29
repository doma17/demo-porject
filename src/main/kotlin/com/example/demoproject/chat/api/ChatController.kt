package com.example.demoproject.chat.api

import com.example.demoproject.chat.application.ChatService
import com.example.demoproject.chat.application.CreateChatCommand
import com.example.demoproject.chat.application.DeleteThreadCommand
import com.example.demoproject.chat.application.ListThreadsQuery
import com.example.demoproject.common.api.ApiResponse
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api")
class ChatController(
    private val chatService: ChatService,
) {
    @PostMapping("/chats")
    fun createChat(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: CreateChatRequest,
    ): ApiResponse<ChatResponse> {
        val result = chatService.createChat(
            CreateChatCommand(
                userId = UUID.fromString(jwt.subject),
                question = request.question,
                isStreaming = request.isStreaming,
                model = request.model,
            ),
        )
        return ApiResponse.success(result.toResponse())
    }

    @GetMapping("/chats")
    fun listThreads(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "lastChatAt,desc") sort: String,
    ): ApiResponse<PageResponse<ThreadResponse>> {
        val (property, direction) = parseSort(sort)
        val result = chatService.listThreads(
            ListThreadsQuery(
                userId = UUID.fromString(jwt.subject),
                page = page,
                size = size,
                sortProperty = property,
                sortDirection = direction,
            ),
        )
        return ApiResponse.success(result.toResponse())
    }

    @DeleteMapping("/threads/{threadId}")
    fun deleteThread(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable threadId: UUID,
    ): ApiResponse<Unit> {
        chatService.deleteThread(DeleteThreadCommand(UUID.fromString(jwt.subject), threadId))
        return ApiResponse.success()
    }

    private fun parseSort(sort: String): Pair<String, String> {
        val parts = sort.split(",", limit = 2)
        return parts[0] to parts.getOrElse(1) { "desc" }
    }
}
