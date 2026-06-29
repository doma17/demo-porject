package com.example.demoproject.ai

import com.example.demoproject.common.error.AiProviderException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.time.Duration

@Component
@EnableConfigurationProperties(OpenAiProperties::class)
class OpenAiResponsesClient(
    private val properties: OpenAiProperties,
    restClientBuilder: RestClient.Builder,
    private val objectMapper: ObjectMapper,
) : AiClient {
    private val restClient = restClientBuilder
        .baseUrl(properties.baseUrl.trimEnd('/'))
        .build()

    override fun complete(command: AiCompletionCommand): AiCompletionResult {
        val apiKey = properties.apiKey.trim()
        if (apiKey.isBlank()) throw AiProviderException("AI_API_KEY is not configured")

        val model = command.model?.takeIf { it.isNotBlank() } ?: properties.model
        val body = mapOf(
            "model" to model,
            "input" to command.messages.map { mapOf("role" to it.role, "content" to it.content) },
        )

        val raw = try {
            restClient.post()
                .uri("/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .headers { it.setBearerAuth(apiKey) }
                .body(body)
                .retrieve()
                .body(String::class.java)
        } catch (ex: RestClientException) {
            throw AiProviderException("AI provider request failed", ex)
        } ?: throw AiProviderException("AI provider returned an empty response")

        val answer = extractOutputText(raw).trim()
        if (answer.isBlank()) throw AiProviderException("AI provider returned an empty answer")
        return AiCompletionResult(answer = answer, model = model)
    }

    private fun extractOutputText(raw: String): String {
        val root = try {
            objectMapper.readTree(raw)
        } catch (ex: Exception) {
            throw AiProviderException("AI provider returned invalid JSON", ex)
        }

        root.path("output_text").takeIf { it.isTextual }?.asText()?.let { return it }

        val output = root.path("output")
        if (output.isArray) {
            val parts = mutableListOf<String>()
            output.forEach { item -> collectText(item, parts) }
            if (parts.isNotEmpty()) return parts.joinToString("\n")
        }

        throw AiProviderException("AI provider response did not include output text")
    }

    private fun collectText(node: JsonNode, parts: MutableList<String>) {
        if (node.isObject) {
            val type = node.path("type").asText("")
            if ((type == "output_text" || type == "text") && node.path("text").isTextual) {
                parts += node.path("text").asText()
            }
            val content = node.path("content")
            if (content.isArray) content.forEach { collectText(it, parts) }
        } else if (node.isArray) {
            node.forEach { collectText(it, parts) }
        }
    }
}
