package com.example.demoproject.common.error

open class ApiException(
    val errorCode: String,
    override val message: String,
) : RuntimeException(message)

class DuplicateEmailException : ApiException("DUPLICATE_EMAIL", "Email is already registered")
class InvalidCredentialsException : ApiException("INVALID_CREDENTIALS", "Invalid email or password")
class InvalidRefreshTokenException : ApiException("INVALID_REFRESH_TOKEN", "Refresh token is invalid")
class ForbiddenException : ApiException("FORBIDDEN", "Access is denied")
class ThreadNotFoundException : ApiException("THREAD_NOT_FOUND", "Thread was not found")
class ChatNotFoundException : ApiException("CHAT_NOT_FOUND", "Chat was not found")
class StreamingNotReadyException : ApiException("STREAMING_NOT_READY", "Streaming chat responses are not available in this demo build")
class AiProviderException(message: String = "AI provider request failed", cause: Throwable? = null) : ApiException("AI_PROVIDER_ERROR", message) {
    init {
        if (cause != null) initCause(cause)
    }
}
class DuplicateFeedbackException : ApiException("DUPLICATE_FEEDBACK", "Feedback already exists for this chat")
class FeedbackNotFoundException : ApiException("FEEDBACK_NOT_FOUND", "Feedback was not found")
class ForbiddenOperationException(message: String = "You do not have permission to perform this operation") : ApiException("FORBIDDEN", message)

