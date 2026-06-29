package com.example.demoproject.common.error

import com.example.demoproject.common.api.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
	@ExceptionHandler(BusinessException::class)
	fun handleBusinessException(exception: BusinessException): ResponseEntity<ApiResponse<Nothing>> =
		ResponseEntity.status(exception.status).body(ApiResponse.error(exception.code, exception.message))

	@ExceptionHandler(MethodArgumentNotValidException::class)
	fun handleValidationException(exception: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
		val message = exception.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "Invalid request"
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ApiResponse.error("VALIDATION_ERROR", message))
	}
}
