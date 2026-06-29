package com.example.demoproject.demo

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("unit")
class DemoPageTest {
    @Test
    fun `static demo page contains core customer demo controls`() {
        val html = javaClass.getResource("/static/index.html")?.readText()
        assertNotNull(html)
        requireNotNull(html)

        assertTrue(html.contains("AI Chat Customer Demo"))
        assertTrue(html.contains("/api/auth/login"))
        assertTrue(html.contains("/api/chats"))
        assertTrue(html.contains("/api/feedback"))
        assertTrue(html.contains("/api/admin/analytics/activity"))
        assertTrue(html.contains("/api/admin/reports/chats.csv"))
        assertTrue(html.contains("isStreaming: false"))
    }
}
