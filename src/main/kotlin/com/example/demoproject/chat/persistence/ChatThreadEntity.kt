package com.example.demoproject.chat.persistence

import com.example.demoproject.user.persistence.UserEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "chat_threads")
class ChatThreadEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = createdAt,

    @Column(name = "last_chat_at", nullable = false)
    var lastChatAt: Instant = createdAt,

    @Column(name = "deleted_at")
    var deletedAt: Instant? = null,

    @OneToMany(mappedBy = "thread", cascade = [CascadeType.ALL], orphanRemoval = false)
    var chats: MutableList<ChatEntity> = mutableListOf(),
) {
    fun touch(chatCreatedAt: Instant) {
        lastChatAt = chatCreatedAt
        updatedAt = chatCreatedAt
    }

    fun softDelete(deletedAt: Instant) {
        this.deletedAt = deletedAt
        updatedAt = deletedAt
    }
}
