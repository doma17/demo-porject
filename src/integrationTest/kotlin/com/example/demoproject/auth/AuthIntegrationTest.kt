package com.example.demoproject.auth

import com.example.demoproject.auth.application.AuthService
import com.example.demoproject.auth.persistence.RefreshTokenRepository
import com.example.demoproject.user.persistence.UserRepository
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
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
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Tag("integration")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthIntegrationTest {
    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var refreshTokenRepository: RefreshTokenRepository

    @Autowired
    lateinit var jwtDecoder: JwtDecoder

    @LocalServerPort
    var port: Int = 0

    @Test
    fun `signup login refresh and logout use hashed refresh tokens`() {
        val email = "member-${System.nanoTime()}@example.com"

        val signup = postJson("/api/auth/signup", mapOf("email" to email, "password" to "password123", "name" to "Member"))
        assertEquals(HttpStatus.CREATED, signup.statusCode)
        assertTrue(json(signup.body!!)["success"].asBoolean())

        val savedUser = userRepository.findByEmail(email).orElseThrow()
        assertNotNull(savedUser.id)
        assertNotNull(savedUser.createdAt)
        assertEquals("member", savedUser.role.name)
        assertFalse(savedUser.passwordHash == "password123")

        val login = postJson("/api/auth/login", mapOf("email" to email, "password" to "password123"))
        assertEquals(HttpStatus.OK, login.statusCode)
        val loginBody = json(login.body!!)
        val accessToken = loginBody["data"]["accessToken"].asText()
        val refreshToken = loginBody["data"]["refreshToken"].asText()
        assertTrue(accessToken.isNotBlank())
        assertTrue(refreshToken.isNotBlank())

        val jwt = jwtDecoder.decode(accessToken)
        assertEquals(savedUser.id.toString(), jwt.getClaimAsString("id"))
        assertEquals(email, jwt.getClaimAsString("email"))
        assertEquals("member", jwt.getClaimAsString("role"))
        assertNotNull(jwt.expiresAt)

        val persistedRefresh = refreshTokenRepository.findAll().single { it.user.id == savedUser.id }
        assertEquals(AuthService.hashRefreshToken(refreshToken), persistedRefresh.tokenHash)
        assertFalse(persistedRefresh.tokenHash == refreshToken)

        val deniedMe = restTemplate.getForEntity("http://localhost:$port/api/users/me", String::class.java)
        assertEquals(HttpStatus.UNAUTHORIZED, deniedMe.statusCode)
        assertEquals("UNAUTHORIZED", json(deniedMe.body!!)["error"]["code"].asText())

        val invalidMe = getJson("/api/users/me", "not-a-valid-access-token")
        assertEquals(HttpStatus.UNAUTHORIZED, invalidMe.statusCode)
        assertEquals("UNAUTHORIZED", json(invalidMe.body!!)["error"]["code"].asText())

        val me = getJson("/api/users/me", accessToken)
        assertEquals(HttpStatus.OK, me.statusCode)
        val meBody = json(me.body!!)
        assertEquals(savedUser.id.toString(), meBody["data"]["id"].asText())
        assertEquals(email, meBody["data"]["email"].asText())
        assertEquals("member", meBody["data"]["role"].asText())

        val invalidRefresh = postJson("/api/auth/refresh", mapOf("refreshToken" to "not-a-valid-refresh-token"))
        assertEquals(HttpStatus.UNAUTHORIZED, invalidRefresh.statusCode)
        assertEquals("INVALID_REFRESH_TOKEN", json(invalidRefresh.body!!)["error"]["code"].asText())

        val refresh = postJson("/api/auth/refresh", mapOf("refreshToken" to refreshToken))
        assertEquals(HttpStatus.OK, refresh.statusCode)
        val refreshBody = json(refresh.body!!)
        assertTrue(refreshBody["data"]["accessToken"].asText().isNotBlank())
        assertNull(refreshBody["data"].get("refreshToken"))

        val logout = postJson("/api/auth/logout", mapOf("refreshToken" to refreshToken))
        assertEquals(HttpStatus.OK, logout.statusCode)

        val reuse = postJson("/api/auth/refresh", mapOf("refreshToken" to refreshToken))
        assertEquals(HttpStatus.UNAUTHORIZED, reuse.statusCode)
    }

    @Test
    fun `invalid credentials fail`() {
        val email = "invalid-${System.nanoTime()}@example.com"
        postJson("/api/auth/signup", mapOf("email" to email, "password" to "password123", "name" to "Member"))

        val response = postJson("/api/auth/login", mapOf("email" to email, "password" to "wrong-password"))

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals("INVALID_CREDENTIALS", json(response.body!!)["error"]["code"].asText())
    }

    private fun postJson(path: String, body: Map<String, String>) = restTemplate.exchange(
        "http://localhost:$port$path",
        HttpMethod.POST,
        HttpEntity(objectMapper.writeValueAsString(body), HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }),
        String::class.java,
    )

    private fun getJson(path: String, accessToken: String) = restTemplate.exchange(
        "http://localhost:$port$path",
        HttpMethod.GET,
        HttpEntity(null, HttpHeaders().apply { setBearerAuth(accessToken) }),
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
