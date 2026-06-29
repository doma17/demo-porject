package com.example.demoproject.common.error

open class ApiException(
    val errorCode: String,
    override val message: String,
) : RuntimeException(message)

class DuplicateEmailException : ApiException("DUPLICATE_EMAIL", "Email is already registered")
class InvalidCredentialsException : ApiException("INVALID_CREDENTIALS", "Invalid email or password")
class InvalidRefreshTokenException : ApiException("INVALID_REFRESH_TOKEN", "Refresh token is invalid")
class ForbiddenException : ApiException("FORBIDDEN", "Admin permission is required")
class AiProviderException(
    message: String = "AI provider request failed",
    cause: Throwable? = null,
) : ApiException("AI_PROVIDER_ERROR", message) {
    init {
        if (cause != null) initCause(cause)
    }
}

class StreamingNotReadyException : ApiException("STREAMING_NOT_READY", "Streaming chat is not available in this demo")
class ThreadNotFoundException : ApiException("THREAD_NOT_FOUND", "Thread was not found")
class DuplicateFeedbackException : ApiException("DUPLICATE_FEEDBACK", "Feedback already exists for this chat")
class FeedbackNotFoundException : ApiException("FEEDBACK_NOT_FOUND", "Feedback was not found")
class ForbiddenOperationException(message: String = "Operation is forbidden") : ApiException("FORBIDDEN", message)
