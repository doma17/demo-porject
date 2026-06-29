package com.example.demoproject.user.domain

import java.time.Instant
import java.util.UUID

data class User(
	val id: UUID? = null,
	val email: String,
	val passwordHash: String,
	val name: String,
	val role: UserRole = UserRole.member,
	val createdAt: Instant = Instant.now(),
)
