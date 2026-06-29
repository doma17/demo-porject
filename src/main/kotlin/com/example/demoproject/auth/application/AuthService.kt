package com.example.demoproject.auth.application

import com.example.demoproject.auth.persistence.RefreshTokenEntity
import com.example.demoproject.auth.persistence.RefreshTokenRepository
import com.example.demoproject.auth.security.AuthProperties
import com.example.demoproject.auth.security.JwtTokenProvider
import com.example.demoproject.common.error.DuplicateEmailException
import com.example.demoproject.common.error.InvalidCredentialsException
import com.example.demoproject.common.error.InvalidRefreshTokenException
import com.example.demoproject.user.persistence.UserEntity
import com.example.demoproject.user.persistence.UserRepository
import com.example.demoproject.user.persistence.UserRole
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Instant
import java.util.Base64
import java.util.HexFormat
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val authProperties: AuthProperties,
    private val clock: Clock,
) {
    private val secureRandom = SecureRandom()

    @Transactional
    fun signup(command: SignupCommand): SignupResult {
        val email = normalizeEmail(command.email)
        if (userRepository.existsByEmail(email)) throw DuplicateEmailException()

        val user = userRepository.save(
            UserEntity(
                email = email,
                passwordHash = passwordEncoder.encode(command.password),
                name = command.name.trim(),
                role = UserRole.member,
                createdAt = Instant.now(clock),
            ),
        )
        return SignupResult(
            id = requireNotNull(user.id),
            email = user.email,
            name = user.name,
            role = user.role.name,
            createdAt = user.createdAt,
        )
    }

    @Transactional
    fun login(command: LoginCommand): TokenResult {
        val user = userRepository.findByEmail(normalizeEmail(command.email)).orElseThrow { InvalidCredentialsException() }
        if (!passwordEncoder.matches(command.password, user.passwordHash)) throw InvalidCredentialsException()
        return issueTokens(user)
    }

    @Transactional
    fun refresh(command: RefreshTokenCommand): AccessTokenResult {
        val token = findActiveRefreshToken(command.refreshToken)
        return AccessTokenResult(accessToken = jwtTokenProvider.issueAccessToken(token.user))
    }

    @Transactional
    fun logout(command: LogoutCommand) {
        val token = findActiveRefreshToken(command.refreshToken)
        token.revoke(Instant.now(clock))
        refreshTokenRepository.save(token)
    }

    private fun issueTokens(user: UserEntity): TokenResult {
        val rawRefreshToken = generateRefreshToken()
        refreshTokenRepository.save(
            RefreshTokenEntity(
                user = user,
                tokenHash = hashRefreshToken(rawRefreshToken),
                expiresAt = Instant.now(clock).plus(authProperties.refreshTokenTtl),
                createdAt = Instant.now(clock),
            ),
        )
        return TokenResult(
            accessToken = jwtTokenProvider.issueAccessToken(user),
            refreshToken = rawRefreshToken,
        )
    }

    private fun findActiveRefreshToken(rawToken: String): RefreshTokenEntity {
        val token = refreshTokenRepository.findByTokenHash(hashRefreshToken(rawToken)).orElseThrow { InvalidRefreshTokenException() }
        if (!token.isActive(Instant.now(clock))) throw InvalidRefreshTokenException()
        return token
    }

    private fun normalizeEmail(email: String): String = email.trim().lowercase()

    private fun generateRefreshToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    companion object {
        fun hashRefreshToken(rawToken: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(rawToken.toByteArray())
            return HexFormat.of().formatHex(digest)
        }
    }
}

data class SignupCommand(val email: String, val password: String, val name: String)
data class LoginCommand(val email: String, val password: String)
data class RefreshTokenCommand(val refreshToken: String)
data class LogoutCommand(val refreshToken: String)

data class SignupResult(
    val id: UUID,
    val email: String,
    val name: String,
    val role: String,
    val createdAt: Instant,
)

data class TokenResult(val accessToken: String, val refreshToken: String)
data class AccessTokenResult(val accessToken: String)
