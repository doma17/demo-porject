package com.example.demoproject.chat

import com.example.demoproject.ai.AiClient
import com.example.demoproject.ai.AiCompletionCommand
import com.example.demoproject.ai.AiCompletionResult
import com.example.demoproject.analytics.persistence.ActivityLogRepository
import com.example.demoproject.analytics.persistence.ActivityType
import com.example.demoproject.analytics.persistence.ChatReportEntryRepository
import com.example.demoproject.chat.persistence.ChatRepository
import com.example.demoproject.chat.persistence.ChatThreadRepository
import com.example.demoproject.user.persistence.UserRepository
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant

@Tag("integration")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatIntegrationTest {
    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var threadRepository: ChatThreadRepository

    @Autowired
    lateinit var chatRepository: ChatRepository

    @Autowired
    lateinit var activityLogRepository: ActivityLogRepository

    @Autowired
    lateinit var chatReportEntryRepository: ChatReportEntryRepository

    @LocalServerPort
    var port: Int = 0

    @Test
    fun `authenticated chat creates persists reuses lists and deletes thread`() {
        val token = signupAndLogin("chat-${System.nanoTime()}@example.com")

        val first = postJson("/api/chats", mapOf("question" to "first question"), token)
        assertEquals(HttpStatus.OK, first.statusCode)
        val firstBody = json(first.body!!)
        val firstThreadId = firstBody["data"]["threadId"].asText()
        assertEquals("answer to first question", firstBody["data"]["answer"].asText())

        val second = postJson("/api/chats", mapOf("question" to "second question"), token)
        assertEquals(HttpStatus.OK, second.statusCode)
        assertEquals(firstThreadId, json(second.body!!)["data"]["threadId"].asText())

        val existingThread = threadRepository.findById(java.util.UUID.fromString(firstThreadId)).orElseThrow()
        existingThread.lastChatAt = Instant.now().minusSeconds(31 * 60)
        existingThread.updatedAt = existingThread.lastChatAt
        threadRepository.save(existingThread)

        val third = postJson("/api/chats", mapOf("question" to "third question", "model" to "override-model"), token)
        assertEquals(HttpStatus.OK, third.statusCode)
        val thirdBody = json(third.body!!)
        val secondThreadId = thirdBody["data"]["threadId"].asText()
        assertNotEquals(firstThreadId, secondThreadId)
        assertEquals("override-model", thirdBody["data"]["model"].asText())
        assertEquals(3, chatRepository.count())
        assertEquals(3, chatReportEntryRepository.count())
        assertEquals(3, activityLogRepository.countByTypeAndCreatedAtBetween(ActivityType.CHAT_CREATED, Instant.now().minusSeconds(60), Instant.now().plusSeconds(60)))

        val list = getJson("/api/chats?sort=createdAt,asc&page=0&size=10", token)
        assertEquals(HttpStatus.OK, list.statusCode)
        val threads = json(list.body!!)["data"]["content"]
        assertEquals(2, threads.size())
        assertEquals(2, threads[0]["chats"].size())
        assertEquals("first question", threads[0]["chats"][0]["question"].asText())
        assertEquals("second question", threads[0]["chats"][1]["question"].asText())

        val delete = exchange("/api/threads/$firstThreadId", HttpMethod.DELETE, null, token)
        assertEquals(HttpStatus.OK, delete.statusCode)

        val afterDelete = getJson("/api/chats?page=0&size=10", token)
        assertEquals(HttpStatus.OK, afterDelete.statusCode)
        assertEquals(1, json(afterDelete.body!!)["data"]["content"].size())
    }

    @Test
    fun `streaming chat is explicit not implemented error`() {
        val token = signupAndLogin("stream-${System.nanoTime()}@example.com")

        val response = postJson("/api/chats", mapOf("question" to "stream please", "isStreaming" to true), token)

        assertEquals(HttpStatus.NOT_IMPLEMENTED, response.statusCode)
        assertEquals("STREAMING_NOT_READY", json(response.body!!)["error"]["code"].asText())
        assertEquals(0, chatRepository.count())
    }


    private fun signupAndLogin(email: String): String {
        exchange("/api/auth/signup", HttpMethod.POST, mapOf("email" to email, "password" to "password123", "name" to "Member"), null)
        val login = exchange("/api/auth/login", HttpMethod.POST, mapOf("email" to email, "password" to "password123"), null)
        assertEquals(HttpStatus.OK, login.statusCode)
        return json(login.body!!)["data"]["accessToken"].asText()
    }

    private fun postJson(path: String, body: Map<String, Any>, token: String) = exchange(path, HttpMethod.POST, body, token)

    private fun getJson(path: String, token: String) = exchange(path, HttpMethod.GET, null, token)

    private fun exchange(path: String, method: HttpMethod, body: Map<String, Any>?, token: String?) = restTemplate.exchange(
        "http://localhost:$port$path",
        method,
        HttpEntity(body?.let { objectMapper.writeValueAsString(it) }, HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            if (token != null) setBearerAuth(token)
        }),
        String::class.java,
    )

    private fun json(raw: String): JsonNode = objectMapper.readTree(raw)

    @TestConfiguration
    class FakeAiClientConfig {
        @Bean
        @Primary
        fun fakeAiClient(): AiClient = object : AiClient {
            override fun complete(command: AiCompletionCommand): AiCompletionResult = AiCompletionResult(
                answer = "answer to ${command.messages.last().content}",
                model = command.model ?: "fake-model",
            )
        }
    }

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer<Nothing>("postgres:15.8")

        @DynamicPropertySource
        @JvmStatic
        fun postgresProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.jpa.hibernate.ddl-auto") { "create" }
            registry.add("auth.jwt.secret") { "integration-test-secret-must-be-at-least-32-bytes-long" }
        }
    }
}
