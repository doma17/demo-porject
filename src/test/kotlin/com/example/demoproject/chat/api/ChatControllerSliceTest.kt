package com.example.demoproject.chat.api

import com.example.demoproject.auth.security.SecurityConfig
import com.example.demoproject.chat.application.ChatResult
import com.example.demoproject.chat.application.ChatService
import com.example.demoproject.chat.application.CreateChatCommand
import com.example.demoproject.chat.application.DeleteThreadCommand
import com.example.demoproject.common.error.GlobalExceptionHandler
import com.example.demoproject.common.error.StreamingNotReadyException
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.doThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Instant
import java.util.UUID

@Tag("slice")
@WebMvcTest(ChatController::class)
@Import(SecurityConfig::class, GlobalExceptionHandler::class)
@TestPropertySource(properties = ["auth.jwt.secret=slice-test-secret-must-be-at-least-32-bytes-long"])
@AutoConfigureMockMvc
class ChatControllerSliceTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var chatService: ChatService

    @Test
    fun `create chat rejects unauthenticated request`() {
        mockMvc.post("/api/chats") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"question":"hello"}"""
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.success") { value(false) }
            jsonPath("$.error.code") { value("UNAUTHORIZED") }
        }
    }

    @Test
    fun `create chat validates nonblank question`() {
        mockMvc.post("/api/chats") {
            with(jwt().jwt { it.subject(UUID.randomUUID().toString()) })
            contentType = MediaType.APPLICATION_JSON
            content = """{"question":""}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.success") { value(false) }
            jsonPath("$.error.code") { value("VALIDATION_ERROR") }
        }
    }

    @Test
    fun `create chat returns api response`() {
        val userId = UUID.randomUUID()
        val threadId = UUID.randomUUID()
        val chatId = UUID.randomUUID()
        doReturn(
            ChatResult(
                threadId = threadId,
                chatId = chatId,
                userId = userId,
                question = "hello",
                answer = "hi",
                model = "demo-model",
                createdAt = Instant.parse("2026-06-29T00:00:00Z"),
            ),
        ).`when`(chatService).createChat(anyCreateChatCommand())

        mockMvc.post("/api/chats") {
            with(jwt().jwt { it.subject(userId.toString()) })
            contentType = MediaType.APPLICATION_JSON
            content = """{"question":"hello","model":"demo-model"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.threadId") { value(threadId.toString()) }
            jsonPath("$.data.chatId") { value(chatId.toString()) }
            jsonPath("$.data.answer") { value("hi") }
        }
    }

    @Test
    fun `streaming request returns documented error shape`() {
        doThrow(StreamingNotReadyException()).`when`(chatService).createChat(anyCreateChatCommand())

        mockMvc.post("/api/chats") {
            with(jwt().jwt { it.subject(UUID.randomUUID().toString()) })
            contentType = MediaType.APPLICATION_JSON
            content = """{"question":"hello","isStreaming":true}"""
        }.andExpect {
            status { isNotImplemented() }
            jsonPath("$.success") { value(false) }
            jsonPath("$.error.code") { value("STREAMING_NOT_READY") }
        }
    }

    @Test
    fun `delete thread returns api response`() {
        doNothing().`when`(chatService).deleteThread(anyDeleteThreadCommand())

        mockMvc.delete("/api/threads/{threadId}", UUID.randomUUID()) {
            with(jwt().jwt { it.subject(UUID.randomUUID().toString()) })
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
        }
    }

    @Test
    fun `static demo paths are public while api paths stay protected`() {
        mockMvc.get("/index.html").andExpect { status { isNotFound() } }
        mockMvc.get("/api/chats").andExpect { status { isUnauthorized() } }
    }

    private fun anyCreateChatCommand(): CreateChatCommand =
        any(CreateChatCommand::class.java) ?: CreateChatCommand(UUID.randomUUID(), "hello", false, null)

    private fun anyDeleteThreadCommand(): DeleteThreadCommand =
        any(DeleteThreadCommand::class.java) ?: DeleteThreadCommand(UUID.randomUUID(), UUID.randomUUID())
}
