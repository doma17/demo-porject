package com.example.demoproject.feedback

import com.example.demoproject.feedback.persistence.FeedbackRepository
import com.example.demoproject.user.persistence.UserEntity
import com.example.demoproject.user.persistence.UserRepository
import com.example.demoproject.user.persistence.UserRole
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.util.UUID

@Tag("integration")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FeedbackIntegrationTest {
    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var feedbackRepository: FeedbackRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @LocalServerPort
    var port: Int = 0

    @Test
    fun `feedback can be created listed and resolved by admin`() {
        val memberToken = signupAndLogin("feedback-${System.nanoTime()}@example.com")
        val chatId = UUID.randomUUID()

        val created = postJson("/api/feedback", mapOf("chatId" to chatId, "positive" to false), memberToken)
        assertEquals(HttpStatus.CREATED, created.statusCode)
        val createdBody = json(created.body!!)
        val feedbackId = createdBody["data"]["id"].asText()
        assertEquals(chatId.toString(), createdBody["data"]["chatId"].asText())
        assertEquals("pending", createdBody["data"]["status"].asText())
        assertEquals(1, feedbackRepository.count())

        val duplicate = postJson("/api/feedback", mapOf("chatId" to chatId, "positive" to true), memberToken)
        assertEquals(HttpStatus.CONFLICT, duplicate.statusCode)
        assertEquals("DUPLICATE_FEEDBACK", json(duplicate.body!!)["error"]["code"].asText())

        val memberList = getJson("/api/feedback?positive=false&page=0&size=10&sort=asc", memberToken)
        assertEquals(HttpStatus.OK, memberList.statusCode)
        assertEquals(1, json(memberList.body!!)["data"]["totalElements"].asInt())

        val memberPatch = patchJson("/api/feedback/$feedbackId/status", mapOf("status" to "resolved"), memberToken)
        assertEquals(HttpStatus.FORBIDDEN, memberPatch.statusCode)

        val adminToken = createAdminAndLogin("admin-${System.nanoTime()}@example.com")
        val resolved = patchJson("/api/feedback/$feedbackId/status", mapOf("status" to "resolved"), adminToken)
        assertEquals(HttpStatus.OK, resolved.statusCode)
        assertEquals("resolved", json(resolved.body!!)["data"]["status"].asText())

        val adminList = getJson("/api/feedback?positive=false&page=0&size=10", adminToken)
        assertEquals(HttpStatus.OK, adminList.statusCode)
        assertTrue(json(adminList.body!!)["data"]["feedback"].any { it["id"].asText() == feedbackId })
    }

    private fun signupAndLogin(email: String): String {
        exchange("/api/auth/signup", HttpMethod.POST, mapOf("email" to email, "password" to "password123", "name" to "Member"), null)
        return login(email)
    }

    private fun createAdminAndLogin(email: String): String {
        userRepository.save(
            UserEntity(
                email = email,
                passwordHash = passwordEncoder.encode("password123"),
                name = "Admin",
                role = UserRole.admin,
                createdAt = Instant.now(),
            ),
        )
        return login(email)
    }

    private fun login(email: String): String {
        val login = exchange("/api/auth/login", HttpMethod.POST, mapOf("email" to email, "password" to "password123"), null)
        assertEquals(HttpStatus.OK, login.statusCode)
        return json(login.body!!)["data"]["accessToken"].asText()
    }

    private fun postJson(path: String, body: Map<String, Any>, token: String) = exchange(path, HttpMethod.POST, body, token)

    private fun patchJson(path: String, body: Map<String, Any>, token: String) = exchange(path, HttpMethod.PATCH, body, token)

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
