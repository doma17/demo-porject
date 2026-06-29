package com.example.demoproject.common.api

data class ApiResponse<T>(
	val success: Boolean,
	val data: T? = null,
	val error: ApiError? = null,
) {
	companion object {
		fun <T> success(data: T): ApiResponse<T> = ApiResponse(success = true, data = data)
		fun error(code: String, message: String): ApiResponse<Nothing> =
			ApiResponse(success = false, error = ApiError(code = code, message = message))
	}
}

data class ApiError(
	val code: String,
	val message: String,
)
