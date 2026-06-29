package com.example.demoproject.common.error

open class ApiException(
    val errorCode: String,
    override val message: String,
) : RuntimeException(message)

class DuplicateEmailException : ApiException("DUPLICATE_EMAIL", "Email is already registered")
class InvalidCredentialsException : ApiException("INVALID_CREDENTIALS", "Invalid email or password")
class InvalidRefreshTokenException : ApiException("INVALID_REFRESH_TOKEN", "Refresh token is invalid")
