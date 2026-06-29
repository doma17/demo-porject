package com.example.demoproject.admin

import com.example.demoproject.user.persistence.UserEntity
import com.example.demoproject.user.persistence.UserRepository
import com.example.demoproject.user.persistence.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@Tag("unit")
class AdminBootstrapRunnerTest {
    private val clock = Clock.fixed(Instant.parse("2026-06-29T00:00:00Z"), ZoneOffset.UTC)
    private val userRepository = mock(UserRepository::class.java)
    private val passwordEncoder = mock(PasswordEncoder::class.java)

    @Test
    fun `creates admin from bootstrap properties when absent`() {
        `when`(userRepository.existsByEmail("admin@example.com")).thenReturn(false)
        `when`(passwordEncoder.encode("password123")).thenReturn("encoded")
        val runner = AdminBootstrapRunner(userRepository, passwordEncoder, clock, " ADMIN@example.com ", "password123", " Admin ")

        runner.run(null)

        val captor = ArgumentCaptor.forClass(UserEntity::class.java)
        verify(userRepository).save(captor.capture())
        assertEquals("admin@example.com", captor.value.email)
        assertEquals("encoded", captor.value.passwordHash)
        assertEquals("Admin", captor.value.name)
        assertEquals(UserRole.admin, captor.value.role)
        assertEquals(clock.instant(), captor.value.createdAt)
    }

    @Test
    fun `skips bootstrap when admin already exists`() {
        `when`(userRepository.existsByEmail("admin@example.com")).thenReturn(true)
        val runner = AdminBootstrapRunner(userRepository, passwordEncoder, clock, "admin@example.com", "password123", "Admin")

        runner.run(null)

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(UserEntity::class.java))
    }
}
