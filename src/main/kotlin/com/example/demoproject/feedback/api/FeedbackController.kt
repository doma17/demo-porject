package com.example.demoproject.feedback.api

import com.example.demoproject.common.api.ApiResponse
import com.example.demoproject.feedback.application.CreateFeedbackCommand
import com.example.demoproject.feedback.application.FeedbackListQuery
import com.example.demoproject.feedback.application.FeedbackResult
import com.example.demoproject.feedback.application.FeedbackService
import com.example.demoproject.feedback.application.UpdateFeedbackStatusCommand
import jakarta.validation.Valid
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/feedback")
class FeedbackController(
    private val feedbackService: FeedbackService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: CreateFeedbackRequest,
    ): ApiResponse<FeedbackResponse> {
        val feedback = feedbackService.create(
            CreateFeedbackCommand(
                userId = UUID.fromString(jwt.subject),
                chatId = requireNotNull(request.chatId),
                positive = requireNotNull(request.positive),
            ),
        )
        return ApiResponse.success(feedback.toResponse())
    }

    @GetMapping
    fun list(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @ModelAttribute params: FeedbackPageParams,
        @RequestParam(required = false) positive: Boolean?,
        @RequestParam(defaultValue = "desc") sort: String,
    ): ApiResponse<FeedbackPageResponse> {
        val page = feedbackService.list(
            FeedbackListQuery(
                userId = UUID.fromString(jwt.subject),
                positive = positive,
                page = params.page,
                size = params.size,
                direction = if (sort.equals("asc", ignoreCase = true)) Sort.Direction.ASC else Sort.Direction.DESC,
            ),
        )
        return ApiResponse.success(
            FeedbackPageResponse(
                feedback = page.content.map { it.toResponse() },
                page = page.number,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
            ),
        )
    }

    @PatchMapping("/{feedbackId}/status")
    fun updateStatus(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable feedbackId: UUID,
        @Valid @RequestBody request: UpdateFeedbackStatusRequest,
    ): ApiResponse<FeedbackResponse> {
        val feedback = feedbackService.updateStatus(
            UpdateFeedbackStatusCommand(
                userId = UUID.fromString(jwt.subject),
                feedbackId = feedbackId,
                status = requireNotNull(request.status),
            ),
        )
        return ApiResponse.success(feedback.toResponse())
    }

    private fun FeedbackResult.toResponse(): FeedbackResponse = FeedbackResponse(
        id = id,
        userId = userId,
        chatId = chatId,
        positive = positive,
        status = status,
        createdAt = createdAt,
    )
}
