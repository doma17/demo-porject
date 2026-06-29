package com.example.demoproject.auth.application

import com.example.demoproject.auth.persistence.RefreshTokenEntity
import com.example.demoproject.auth.persistence.RefreshTokenRepository
import com.example.demoproject.auth.security.AuthProperties
import com.example.demoproject.auth.security.JwtTokenProvider
import com.example.demoproject.user.persistence.UserEntity
import com.example.demoproject.user.persistence.UserRepository
import com.example.demoproject.user.persistence.UserRole
import com.nimbusds.jose.jwk.source.ImmutableSecret
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID
import javax.crypto.spec.SecretKeySpec

@Tag("unit")
class AuthServiceTest {
    private val secret = "unit-test-secret-must-be-at-least-32-bytes-long"
    private val properties = AuthProperties(secret = secret, accessTokenTtl = Duration.ofMinutes(15), refreshTokenTtl = Duration.ofDays(7))
    private val clock = Clock.fixed(Instant.parse("2026-06-29T00:00:00Z"), ZoneOffset.UTC)
    private val encoder = BCryptPasswordEncoder()
    private val jwtEncoder = NimbusJwtEncoder(ImmutableSecret(secret.toByteArray()))
    private val jwtDecoder = NimbusJwtDecoder
        .withSecretKey(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        .macAlgorithm(MacAlgorithm.HS256)
        .build()

    @Test
    fun `login issues jwt with user claims and stores hashed refresh token`() {
        val userRepository = mock(UserRepository::class.java)
        val refreshTokenRepository = mock(RefreshTokenRepository::class.java)
        val userId = UUID.randomUUID()
        val user = UserEntity(
            id = userId,
            email = "member@example.com",
            passwordHash = encoder.encode("password123"),
            name = "Member",
            role = UserRole.member,
        )
        `when`(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user))
        `when`(refreshTokenRepository.save(any(RefreshTokenEntity::class.java))).thenAnswer { it.arguments[0] }

        val service = service(userRepository, refreshTokenRepository)
        val result = service.login(LoginCommand("MEMBER@example.com", "password123"))

        val jwt = jwtDecoder.decode(result.accessToken)
        assertEquals(userId.toString(), jwt.subject)
        assertEquals(userId.toString(), jwt.getClaimAsString("id"))
        assertEquals("member@example.com", jwt.getClaimAsString("email"))
        assertEquals("member", jwt.getClaimAsString("role"))
        assertTrue(jwt.expiresAt!!.isAfter(clock.instant()))

        val captor = ArgumentCaptor.forClass(RefreshTokenEntity::class.java)
        verify(refreshTokenRepository).save(captor.capture())
        assertNotEquals(result.refreshToken, captor.value.tokenHash)
        assertEquals(AuthService.hashRefreshToken(result.refreshToken), captor.value.tokenHash)
    }

    @Test
    fun `login rejects invalid password`() {
        val userRepository = mock(UserRepository::class.java)
        val refreshTokenRepository = mock(RefreshTokenRepository::class.java)
        val user = UserEntity(
            id = UUID.randomUUID(),
            email = "member@example.com",
            passwordHash = encoder.encode("password123"),
            name = "Member",
        )
        `when`(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user))

        val service = service(userRepository, refreshTokenRepository)

        assertThrows(com.example.demoproject.common.error.InvalidCredentialsException::class.java) {
            service.login(LoginCommand("member@example.com", "wrong-password"))
        }
    }

    @Test
    fun `revoked refresh token cannot be reused`() {
        val userRepository = mock(UserRepository::class.java)
        val refreshTokenRepository = mock(RefreshTokenRepository::class.java)
        val token = "refresh-token"
        val entity = RefreshTokenEntity(
            user = UserEntity(id = UUID.randomUUID(), email = "member@example.com", passwordHash = "hash", name = "Member"),
            tokenHash = AuthService.hashRefreshToken(token),
            expiresAt = clock.instant().plus(Duration.ofDays(1)),
            revokedAt = clock.instant(),
        )
        `when`(refreshTokenRepository.findByTokenHash(AuthService.hashRefreshToken(token))).thenReturn(Optional.of(entity))

        val service = service(userRepository, refreshTokenRepository)

        assertThrows(com.example.demoproject.common.error.InvalidRefreshTokenException::class.java) {
            service.refresh(RefreshTokenCommand(token))
        }
        assertFalse(entity.isActive(clock.instant()))
    }

    private fun service(userRepository: UserRepository, refreshTokenRepository: RefreshTokenRepository): AuthService = AuthService(
        userRepository = userRepository,
        refreshTokenRepository = refreshTokenRepository,
        passwordEncoder = encoder,
        jwtTokenProvider = JwtTokenProvider(jwtEncoder, properties, clock),
        authProperties = properties,
        clock = clock,
    )
}
