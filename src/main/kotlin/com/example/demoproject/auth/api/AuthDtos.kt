package com.example.demoproject.auth.api

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class SignupRequest(
    @field:Email
    @field:NotBlank
    val email: String,
    @field:NotBlank
    @field:Size(min = 8)
    val password: String,
    @field:NotBlank
    val name: String,
)

data class LoginRequest(
    @field:Email
    @field:NotBlank
    val email: String,
    @field:NotBlank
    val password: String,
)

data class RefreshTokenRequest(
    @field:NotBlank
    val refreshToken: String,
)

data class LogoutRequest(
    @field:NotBlank
    val refreshToken: String,
)

data class SignupResponse(
    val id: UUID,
    val email: String,
    val name: String,
    val role: String,
    val createdAt: Instant,
)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
)

data class AccessTokenResponse(
    val accessToken: String,
)
