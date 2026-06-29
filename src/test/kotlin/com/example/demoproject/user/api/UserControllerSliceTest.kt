package com.example.demoproject.user.api

import com.example.demoproject.auth.security.SecurityConfig
import com.example.demoproject.user.application.CurrentUserResult
import com.example.demoproject.user.application.CurrentUserService
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doReturn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.Instant
import java.util.UUID

@Tag("slice")
@WebMvcTest(UserController::class)
@Import(SecurityConfig::class)
@TestPropertySource(properties = ["auth.jwt.secret=slice-test-secret-must-be-at-least-32-bytes-long"])
@AutoConfigureMockMvc
class UserControllerSliceTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var currentUserService: CurrentUserService

    @Test
    fun `me rejects invalid bearer token`() {
        mockMvc.get("/api/users/me") {
            accept = MediaType.APPLICATION_JSON
            header(HttpHeaders.AUTHORIZATION, "Bearer not-a-valid-access-token")
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.success") { value(false) }
            jsonPath("$.error.code") { value("UNAUTHORIZED") }
        }
    }

    @Test
    fun `me returns current user api response`() {
        val userId = UUID.fromString("00000000-0000-0000-0000-000000000001")
        doReturn(
            CurrentUserResult(
                id = userId,
                email = "member@example.com",
                name = "Member",
                role = "member",
                createdAt = Instant.parse("2026-06-29T00:00:00Z"),
            ),
        ).`when`(currentUserService).getCurrentUser(userId)

        mockMvc.get("/api/users/me") {
            accept = MediaType.APPLICATION_JSON
            with(jwt().jwt { it.subject(userId.toString()) })
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.id") { value(userId.toString()) }
            jsonPath("$.data.email") { value("member@example.com") }
            jsonPath("$.data.role") { value("member") }
        }
    }
}
