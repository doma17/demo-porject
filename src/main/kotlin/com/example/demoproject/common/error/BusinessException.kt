package com.example.demoproject.common.error

import org.springframework.http.HttpStatus

open class BusinessException(
	val code: String,
	override val message: String,
	val status: HttpStatus,
) : RuntimeException(message)

class DuplicateEmailException(email: String) : BusinessException(
	code = "DUPLICATE_EMAIL",
	message = "Email already exists: $email",
	status = HttpStatus.CONFLICT,
)
