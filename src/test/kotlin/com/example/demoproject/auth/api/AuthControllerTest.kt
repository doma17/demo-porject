package com.example.demoproject.auth.api

import com.example.demoproject.auth.application.AuthService
import com.example.demoproject.auth.application.SignupCommand
import com.example.demoproject.auth.application.SignupResult
import com.example.demoproject.common.error.DuplicateEmailException
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.doThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.Instant
import java.util.UUID

@Tag("slice")
@WebMvcTest(AuthController::class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {
	@Autowired
	private lateinit var mockMvc: MockMvc

	@MockitoBean
	private lateinit var authService: AuthService

	@Test
	fun `signup returns api response`() {
		doReturn(
			SignupResult(
				id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
				email = "user@example.com",
				name = "User",
				role = "member",
				createdAt = Instant.parse("2026-06-29T00:00:00Z"),
			),
		).`when`(authService).signup(anySignupCommand())

		mockMvc.post("/api/auth/signup") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"email":"user@example.com","password":"password123","name":"User"}"""
		}.andExpect {
			status { isCreated() }
			jsonPath("$.success") { value(true) }
			jsonPath("$.data.email") { value("user@example.com") }
			jsonPath("$.data.role") { value("member") }
		}
	}

	@Test
	fun `signup validation returns error response`() {
		mockMvc.post("/api/auth/signup") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"email":"bad","password":"short","name":""}"""
		}.andExpect {
			status { isBadRequest() }
			jsonPath("$.success") { value(false) }
			jsonPath("$.error.code") { value("VALIDATION_ERROR") }
		}
	}

	@Test
	fun `duplicate email returns conflict error response`() {
		doThrow(DuplicateEmailException("user@example.com")).`when`(authService).signup(anySignupCommand())

		mockMvc.post("/api/auth/signup") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"email":"user@example.com","password":"password123","name":"User"}"""
		}.andExpect {
			status { isConflict() }
			jsonPath("$.success") { value(false) }
			jsonPath("$.error.code") { value("DUPLICATE_EMAIL") }
		}
	}

	private fun anySignupCommand(): SignupCommand =
		any(SignupCommand::class.java) ?: SignupCommand("user@example.com", "password123", "User")
}
