package com.example.demoproject.auth.api

import com.example.demoproject.auth.application.AuthService
import com.example.demoproject.auth.application.SignupCommand
import com.example.demoproject.auth.application.SignupResult
import com.example.demoproject.common.api.ApiResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/auth")
class AuthController(
	private val authService: AuthService,
) {
	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	fun signup(@Valid @RequestBody request: SignupRequest): ApiResponse<SignupResponse> {
		val result = authService.signup(request.toCommand())
		return ApiResponse.success(result.toResponse())
	}
}

data class SignupRequest(
	field:NotBlank(message = "Email is required")
	field:Email(message = "Email must be valid")
	val email: String,

	field:NotBlank(message = "Password is required")
	field:Size(min = 8, message = "Password must be at least 8 characters")
	val password: String,

	field:NotBlank(message = "Name is required")
	val name: String,
) {
	fun toCommand(): SignupCommand = SignupCommand(
		email = email,
		password = password,
		name = name,
	)
}

data class SignupResponse(
	val id: UUID,
	val email: String,
	val name: String,
	val role: String,
	val createdAt: Instant,
)

private fun SignupResult.toResponse(): SignupResponse = SignupResponse(
	id = id,
	email = email,
	name = name,
	role = role,
	createdAt = createdAt,
)
