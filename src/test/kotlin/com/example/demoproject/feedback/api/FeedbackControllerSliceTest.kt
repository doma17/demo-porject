package com.example.demoproject.feedback.api

import com.example.demoproject.auth.security.SecurityConfig
import com.example.demoproject.common.error.GlobalExceptionHandler
import com.example.demoproject.feedback.application.CreateFeedbackCommand
import com.example.demoproject.feedback.application.FeedbackListQuery
import com.example.demoproject.feedback.application.FeedbackResult
import com.example.demoproject.feedback.application.FeedbackService
import com.example.demoproject.feedback.application.UpdateFeedbackStatusCommand
import com.example.demoproject.feedback.persistence.FeedbackStatus
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doReturn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.time.Instant
import java.util.UUID

@Tag("slice")
@WebMvcTest(FeedbackController::class)
@Import(SecurityConfig::class, GlobalExceptionHandler::class)
@TestPropertySource(properties = ["auth.jwt.secret=slice-test-secret-must-be-at-least-32-bytes-long"])
@AutoConfigureMockMvc
class FeedbackControllerSliceTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var feedbackService: FeedbackService

    @Test
    fun `create returns feedback api response`() {
        val userId = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val chatId = UUID.fromString("00000000-0000-0000-0000-000000000002")
        doReturn(result(userId = userId, chatId = chatId, positive = true)).`when`(feedbackService).create(anyCreateCommand())

        mockMvc.post("/api/feedback") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("chatId" to chatId, "positive" to true))
            with(jwt().jwt { it.subject(userId.toString()).claim("role", "member") })
        }.andExpect {
            status { isCreated() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.chatId") { value(chatId.toString()) }
            jsonPath("$.data.status") { value("pending") }
        }
    }

    @Test
    fun `create rejects missing chat id`() {
        val userId = UUID.fromString("00000000-0000-0000-0000-000000000001")

        mockMvc.post("/api/feedback") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("positive" to true))
            with(jwt().jwt { it.subject(userId.toString()).claim("role", "member") })
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.success") { value(false) }
            jsonPath("$.error.code") { value("VALIDATION_ERROR") }
        }
    }

    @Test
    fun `list returns paged feedback response`() {
        val userId = UUID.fromString("00000000-0000-0000-0000-000000000001")
        doReturn(PageImpl(listOf(result(userId = userId)), PageRequest.of(0, 20), 1)).`when`(feedbackService).list(anyFeedbackListQuery())

        mockMvc.get("/api/feedback") {
            param("positive", "true")
            param("sort", "asc")
            with(jwt().jwt { it.subject(userId.toString()).claim("role", "member") })
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.feedback[0].userId") { value(userId.toString()) }
            jsonPath("$.data.totalElements") { value(1) }
        }
    }

    @Test
    fun `admin status update returns resolved feedback`() {
        val adminId = UUID.fromString("00000000-0000-0000-0000-000000000003")
        val feedbackId = UUID.fromString("00000000-0000-0000-0000-000000000004")
        doReturn(result(id = feedbackId, userId = adminId, status = FeedbackStatus.resolved)).`when`(feedbackService).updateStatus(anyUpdateFeedbackStatusCommand())

        mockMvc.patch("/api/feedback/$feedbackId/status") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("status" to "resolved"))
            with(jwt().jwt { it.subject(adminId.toString()).claim("role", "admin") })
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.status") { value("resolved") }
        }
    }

    private fun result(
        id: UUID = UUID.fromString("00000000-0000-0000-0000-000000000101"),
        userId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001"),
        chatId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002"),
        positive: Boolean = true,
        status: FeedbackStatus = FeedbackStatus.pending,
    ): FeedbackResult = FeedbackResult(
        id = id,
        userId = userId,
        chatId = chatId,
        positive = positive,
        status = status,
        createdAt = Instant.parse("2026-06-29T00:00:00Z"),
    )

    private fun anyCreateCommand(): CreateFeedbackCommand =
        any(CreateFeedbackCommand::class.java) ?: CreateFeedbackCommand(UUID.randomUUID(), UUID.randomUUID(), true)

    private fun anyFeedbackListQuery(): FeedbackListQuery =
        any(FeedbackListQuery::class.java) ?: FeedbackListQuery(UUID.randomUUID(), null, 0, 20, org.springframework.data.domain.Sort.Direction.DESC)

    private fun anyUpdateFeedbackStatusCommand(): UpdateFeedbackStatusCommand =
        any(UpdateFeedbackStatusCommand::class.java) ?: UpdateFeedbackStatusCommand(UUID.randomUUID(), UUID.randomUUID(), FeedbackStatus.resolved)
}
