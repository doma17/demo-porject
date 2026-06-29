package com.example.demoproject.analytics.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "activity_logs")
class ActivityLogEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: ActivityType = ActivityType.SIGNUP,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
)

enum class ActivityType { SIGNUP, LOGIN, CHAT_CREATED }
