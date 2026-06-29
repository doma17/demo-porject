package com.example.demoproject.admin

import com.example.demoproject.analytics.application.ActivityStats
import com.example.demoproject.analytics.application.AnalyticsService
import com.example.demoproject.analytics.application.ChatReportService
import com.example.demoproject.common.error.GlobalExceptionHandler
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doReturn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.Instant

@Tag("slice")
@WebMvcTest(AdminAnalyticsController::class)
@Import(GlobalExceptionHandler::class)
@AutoConfigureMockMvc(addFilters = false)
class AdminAnalyticsControllerSliceTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var analyticsService: AnalyticsService

    @MockitoBean
    private lateinit var chatReportService: ChatReportService

    @Test
    fun `admin can get activity stats`() {
        doReturn(
            ActivityStats(
                since = Instant.parse("2026-06-28T12:00:00Z"),
                until = Instant.parse("2026-06-29T12:00:00Z"),
                signupCount = 2,
                loginCount = 3,
                chatCreatedCount = 4,
            ),
        ).`when`(analyticsService).getLastDayActivity()

        mockMvc.get("/api/admin/analytics/activity") {
            with(jwt().jwt { it.claim("role", "admin") })
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.signupCount") { value(2) }
            jsonPath("$.data.loginCount") { value(3) }
            jsonPath("$.data.chatCreatedCount") { value(4) }
        }
    }

    @Test
    fun `member cannot get activity stats`() {
        mockMvc.get("/api/admin/analytics/activity") {
            with(jwt().jwt { it.claim("role", "member") })
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.success") { value(false) }
            jsonPath("$.error.code") { value("FORBIDDEN") }
        }
    }

    @Test
    fun `admin can download chat report csv`() {
        doReturn("createdAt,userId,email,name,question,answer\n").`when`(chatReportService).generateLastDayCsv()

        mockMvc.get("/api/admin/reports/chats.csv") {
            with(jwt().jwt { it.claim("role", "admin") })
        }.andExpect {
            status { isOk() }
            content { contentType("text/csv") }
            content { string("createdAt,userId,email,name,question,answer\n") }
        }
    }
}
