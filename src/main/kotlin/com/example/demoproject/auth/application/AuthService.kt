package com.example.demoproject.auth.application

import com.example.demoproject.common.error.DuplicateEmailException
import com.example.demoproject.user.application.UserRepository
import com.example.demoproject.user.domain.User
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class AuthService(
	private val userRepository: UserRepository,
	private val passwordEncoder: PasswordEncoder,
) {
	@Transactional
	fun signup(command: SignupCommand): SignupResult {
		val email = command.email.trim().lowercase()
		if (userRepository.existsByEmail(email)) {
			throw DuplicateEmailException(email)
		}

		val user = User(
			email = email,
			passwordHash = passwordEncoder.encode(command.password),
			name = command.name.trim(),
		)

		val saved = try {
			userRepository.save(user)
		} catch (exception: DataIntegrityViolationException) {
			throw DuplicateEmailException(email)
		}

		return SignupResult(
			id = requireNotNull(saved.id),
			email = saved.email,
			name = saved.name,
			role = saved.role.name,
			createdAt = saved.createdAt,
		)
	}
}

data class SignupCommand(
	val email: String,
	val password: String,
	val name: String,
)

data class SignupResult(
	val id: UUID,
	val email: String,
	val name: String,
	val role: String,
	val createdAt: Instant,
)
