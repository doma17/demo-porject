package com.example.demoproject.ai

interface AiClient {
    fun complete(command: AiCompletionCommand): AiCompletionResult
}

data class AiCompletionCommand(
    val model: String?,
    val messages: List<AiMessage>,
)

data class AiMessage(
    val role: String,
    val content: String,
)

data class AiCompletionResult(
    val answer: String,
    val model: String,
)
