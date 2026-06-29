package com.example.demoproject.feedback.persistence

import com.example.demoproject.user.persistence.UserEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "feedback",
    uniqueConstraints = [UniqueConstraint(name = "uk_feedback_user_chat", columnNames = ["user_id", "chat_id"])],
)
class FeedbackEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity = UserEntity(),

    @Column(name = "chat_id", nullable = false)
    var chatId: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    var positive: Boolean = true,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: FeedbackStatus = FeedbackStatus.pending,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
) {
    fun resolve(status: FeedbackStatus) {
        this.status = status
    }
}

enum class FeedbackStatus { pending, resolved }
