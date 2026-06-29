package com.example.demoproject.feedback.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface FeedbackRepository : JpaRepository<FeedbackEntity, UUID> {
    fun existsByUser_IdAndChatId(userId: UUID, chatId: UUID): Boolean
    fun findByUser_Id(userId: UUID, pageable: Pageable): Page<FeedbackEntity>
    fun findByPositive(positive: Boolean, pageable: Pageable): Page<FeedbackEntity>
    fun findByUser_IdAndPositive(userId: UUID, positive: Boolean, pageable: Pageable): Page<FeedbackEntity>
}
