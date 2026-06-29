package com.example.demoproject.demo

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Tag("integration")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoPageIntegrationTest {
    @LocalServerPort
    var port: Int = 0

    private val restTemplate = TestRestTemplate()

    @Test
    fun `demo page is public without authentication`() {
        val root = restTemplate.getForEntity("http://localhost:$port/", String::class.java)
        val index = restTemplate.getForEntity("http://localhost:$port/index.html", String::class.java)

        assertTrue(root.statusCode == HttpStatus.OK, "Expected / to be public, got ${root.statusCode}: ${root.body}")
        assertTrue(index.statusCode == HttpStatus.OK, "Expected /index.html to be public, got ${index.statusCode}: ${index.body}")
        assertTrue(root.body.orEmpty().contains("AI Chat Customer Demo"))
        assertTrue(index.body.orEmpty().contains("AI Chat Customer Demo"))
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
