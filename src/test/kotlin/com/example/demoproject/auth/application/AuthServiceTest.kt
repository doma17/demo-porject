package com.example.demoproject.auth.application

import com.example.demoproject.common.error.DuplicateEmailException
import com.example.demoproject.user.application.UserRepository
import com.example.demoproject.user.domain.User
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.UUID

@Tag("unit")
class AuthServiceTest {
	private val passwordEncoder = BCryptPasswordEncoder()

	@Test
	fun `signup hashes password and normalizes email`() {
		val repository = FakeUserRepository()
		val service = AuthService(repository, passwordEncoder)

		val result = service.signup(SignupCommand(" USER@Example.COM ", "password123", " User "))

		val saved = repository.saved.single()
		assertNotEquals("password123", saved.passwordHash)
		assertTrue(passwordEncoder.matches("password123", saved.passwordHash))
		assertTrue(result.email == "user@example.com")
	}

	@Test
	fun `signup rejects duplicate email`() {
		val repository = FakeUserRepository(existingEmails = mutableSetOf("user@example.com"))
		val service = AuthService(repository, passwordEncoder)

		assertThrows(DuplicateEmailException::class.java) {
			service.signup(SignupCommand("user@example.com", "password123", "User"))
		}
	}

	private class FakeUserRepository(
		private val existingEmails: MutableSet<String> = mutableSetOf(),
	) : UserRepository {
		val saved = mutableListOf<User>()

		override fun existsByEmail(email: String): Boolean = email in existingEmails

		override fun save(user: User): User {
			val savedUser = user.copy(id = UUID.randomUUID())
			saved += savedUser
			existingEmails += savedUser.email
			return savedUser
		}
	}
}
