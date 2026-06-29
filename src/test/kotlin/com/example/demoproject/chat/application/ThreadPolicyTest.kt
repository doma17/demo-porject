package com.example.demoproject.chat.application

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.Instant

@Tag("unit")
class ThreadPolicyTest {
    private val policy = ThreadPolicy()
    private val now = Instant.parse("2026-06-29T00:30:00Z")

    @Test
    fun `reuses thread when last chat is within thirty minutes`() {
        assertTrue(policy.shouldReuse(now.minusSeconds(29 * 60), now))
    }

    @Test
    fun `reuses thread exactly at thirty minute boundary`() {
        assertTrue(policy.shouldReuse(now.minusSeconds(30 * 60), now))
    }

    @Test
    fun `creates new thread when last chat is older than thirty minutes`() {
        assertFalse(policy.shouldReuse(now.minusSeconds(31 * 60), now))
    }
}
