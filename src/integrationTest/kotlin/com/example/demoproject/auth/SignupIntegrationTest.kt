package com.example.demoproject.auth

import com.example.demoproject.user.persistence.SpringDataUserJpaRepository
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Tag("integration")
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class SignupIntegrationTest {
	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var userJpaRepository: SpringDataUserJpaRepository

	@Autowired
	private lateinit var passwordEncoder: PasswordEncoder

	@Test
	fun `signup persists user with hashed password and member role`() {
		mockMvc.post("/api/auth/signup") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"email":"user@example.com","password":"password123","name":"User"}"""
		}.andExpect {
			status { isCreated() }
			jsonPath("$.success") { value(true) }
			jsonPath("$.data.email") { value("user@example.com") }
			jsonPath("$.data.role") { value("member") }
		}

		val saved = requireNotNull(userJpaRepository.findByEmail("user@example.com"))
		assertNotEquals("password123", saved.passwordHash)
		assertTrue(passwordEncoder.matches("password123", saved.passwordHash))
		assertTrue(saved.role.name == "member")
	}

	@Test
	fun `duplicate signup fails and does not duplicate user`() {
		signup("duplicate@example.com")

		mockMvc.post("/api/auth/signup") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"email":"duplicate@example.com","password":"password123","name":"User"}"""
		}.andExpect {
			status { isConflict() }
			jsonPath("$.success") { value(false) }
			jsonPath("$.error.code") { value("DUPLICATE_EMAIL") }
		}

		assertTrue(userJpaRepository.findAll().count { it.email == "duplicate@example.com" } == 1)
	}

	private fun signup(email: String) {
		mockMvc.post("/api/auth/signup") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"email":"$email","password":"password123","name":"User"}"""
		}.andExpect { status { isCreated() } }
	}

	companion object {
		@Container
		@JvmStatic
		val postgres: PostgreSQLContainer<Nothing> = PostgreSQLContainer<Nothing>("postgres:15.8")

		@DynamicPropertySource
		@JvmStatic
		fun properties(registry: DynamicPropertyRegistry) {
			registry.add("spring.datasource.url", postgres::getJdbcUrl)
			registry.add("spring.datasource.username", postgres::getUsername)
			registry.add("spring.datasource.password", postgres::getPassword)
			registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }
		}
	}
}
