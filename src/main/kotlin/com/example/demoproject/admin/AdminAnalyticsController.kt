package com.example.demoproject.admin

import com.example.demoproject.analytics.application.ActivityStats
import com.example.demoproject.analytics.application.AnalyticsService
import com.example.demoproject.analytics.application.ChatReportService
import com.example.demoproject.common.api.ApiResponse
import com.example.demoproject.common.error.ForbiddenException
import com.example.demoproject.user.persistence.UserRole
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/admin")
class AdminAnalyticsController(
    private val analyticsService: AnalyticsService,
    private val chatReportService: ChatReportService,
) {
    @GetMapping("/analytics/activity")
    fun activity(@AuthenticationPrincipal jwt: Jwt): ApiResponse<ActivityStatsResponse> {
        requireAdmin(jwt)
        return ApiResponse.success(analyticsService.getLastDayActivity().toResponse())
    }

    @GetMapping("/reports/chats.csv", produces = ["text/csv"])
    fun chatReport(@AuthenticationPrincipal jwt: Jwt): ResponseEntity<String> {
        requireAdmin(jwt)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"chat-report.csv\"")
            .body(chatReportService.generateLastDayCsv())
    }

    private fun requireAdmin(jwt: Jwt) {
        if (jwt.getClaimAsString("role") != UserRole.admin.name) throw ForbiddenException()
    }

    private fun ActivityStats.toResponse(): ActivityStatsResponse =
        ActivityStatsResponse(
            since = since,
            until = until,
            signupCount = signupCount,
            loginCount = loginCount,
            chatCreatedCount = chatCreatedCount,
        )
}

data class ActivityStatsResponse(
    val since: Instant,
    val until: Instant,
    val signupCount: Long,
    val loginCount: Long,
    val chatCreatedCount: Long,
)
