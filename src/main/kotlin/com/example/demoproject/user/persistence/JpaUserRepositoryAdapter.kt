package com.example.demoproject.user.persistence

import com.example.demoproject.user.application.UserRepository
import com.example.demoproject.user.domain.User
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Repository

@Repository
class JpaUserRepositoryAdapter(
	private val userJpaRepository: SpringDataUserJpaRepository,
) : UserRepository {
	override fun existsByEmail(email: String): Boolean = userJpaRepository.existsByEmail(email)

	override fun save(user: User): User = try {
		userJpaRepository.save(UserEntity.from(user)).toDomain()
	} catch (exception: DataIntegrityViolationException) {
		throw exception
	}
}
