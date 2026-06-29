package com.example.demoproject.common.error

import com.example.demoproject.common.api.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(DuplicateEmailException::class)
    fun duplicateEmail(ex: DuplicateEmailException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.errorCode, ex.message))

    @ExceptionHandler(InvalidCredentialsException::class)
    fun invalidCredentials(ex: InvalidCredentialsException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ex.errorCode, ex.message))

    @ExceptionHandler(InvalidRefreshTokenException::class)
    fun invalidRefreshToken(ex: InvalidRefreshTokenException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ex.errorCode, ex.message))

    @ExceptionHandler(JwtException::class)
    fun invalidJwt(ex: JwtException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("INVALID_TOKEN", ex.message ?: "Invalid token"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validation(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val message = ex.bindingResult.allErrors.joinToString("; ") { error ->
            val field = (error as? FieldError)?.field ?: error.objectName
            "$field ${error.defaultMessage ?: "is invalid"}"
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("VALIDATION_ERROR", message))
    }
}
