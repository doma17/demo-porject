package com.example.demoproject.feedback.api

import com.example.demoproject.feedback.persistence.FeedbackStatus
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

data class CreateFeedbackRequest(
    @field:NotNull
    val chatId: UUID?,
    @field:NotNull
    val positive: Boolean?,
)

data class UpdateFeedbackStatusRequest(
    @field:NotNull
    val status: FeedbackStatus?,
)

data class FeedbackResponse(
    val id: UUID,
    val userId: UUID,
    val chatId: UUID,
    val positive: Boolean,
    val status: FeedbackStatus,
    val createdAt: Instant,
)

data class FeedbackPageResponse(
    val feedback: List<FeedbackResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

data class FeedbackPageParams(
    @field:Min(0)
    val page: Int = 0,
    @field:Min(1)
    @field:Max(100)
    val size: Int = 20,
)
