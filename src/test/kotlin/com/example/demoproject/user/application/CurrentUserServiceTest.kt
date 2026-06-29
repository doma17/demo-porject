package com.example.demoproject.user.application

import com.example.demoproject.common.error.InvalidCredentialsException
import com.example.demoproject.user.persistence.UserEntity
import com.example.demoproject.user.persistence.UserRepository
import com.example.demoproject.user.persistence.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Instant
import java.util.Optional
import java.util.UUID

@Tag("unit")
class CurrentUserServiceTest {
    private val userRepository = mock(UserRepository::class.java)
    private val service = CurrentUserService(userRepository)

    @Test
    fun `returns current user from repository`() {
        val userId = UUID.randomUUID()
        `when`(userRepository.findById(userId)).thenReturn(
            Optional.of(
                UserEntity(
                    id = userId,
                    email = "member@example.com",
                    passwordHash = "hash",
                    name = "Member",
                    role = UserRole.member,
                    createdAt = Instant.parse("2026-06-29T00:00:00Z"),
                ),
            ),
        )

        val result = service.getCurrentUser(userId)

        assertEquals(userId, result.id)
        assertEquals("member@example.com", result.email)
        assertEquals("Member", result.name)
        assertEquals("member", result.role)
    }

    @Test
    fun `missing current user is rejected`() {
        val userId = UUID.randomUUID()
        `when`(userRepository.findById(userId)).thenReturn(Optional.empty())

        assertThrows(InvalidCredentialsException::class.java) {
            service.getCurrentUser(userId)
        }
    }
}
