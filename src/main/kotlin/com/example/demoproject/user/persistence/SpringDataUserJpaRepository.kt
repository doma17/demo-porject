package com.example.demoproject.user.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataUserJpaRepository : JpaRepository<UserEntity, UUID> {
	fun existsByEmail(email: String): Boolean
	fun findByEmail(email: String): UserEntity?
}
