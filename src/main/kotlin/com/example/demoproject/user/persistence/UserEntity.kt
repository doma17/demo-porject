package com.example.demoproject.user.persistence

import com.example.demoproject.user.domain.User
import com.example.demoproject.user.domain.UserRole
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
@Table(name = "users")
class UserEntity(
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	var id: UUID? = null,

	@Column(nullable = false, unique = true)
	var email: String,

	@Column(name = "password_hash", nullable = false)
	var passwordHash: String,

	@Column(nullable = false)
	var name: String,

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	var role: UserRole = UserRole.member,

	@Column(name = "created_at", nullable = false)
	var createdAt: Instant = Instant.now(),
) {
	fun toDomain(): User = User(
		id = id,
		email = email,
		passwordHash = passwordHash,
		name = name,
		role = role,
		createdAt = createdAt,
	)

	companion object {
		fun from(user: User): UserEntity = UserEntity(
			id = user.id,
			email = user.email,
			passwordHash = user.passwordHash,
			name = user.name,
			role = user.role,
			createdAt = user.createdAt,
		)
	}
}
