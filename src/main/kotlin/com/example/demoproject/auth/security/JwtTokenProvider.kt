package com.example.demoproject.auth.security

import com.example.demoproject.user.persistence.UserEntity
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant

@Component
class JwtTokenProvider(
    private val jwtEncoder: JwtEncoder,
    private val authProperties: AuthProperties,
    private val clock: Clock,
) {
    fun issueAccessToken(user: UserEntity): String {
        val now = Instant.now(clock)
        val userId = requireNotNull(user.id) { "User id is required before issuing a token" }
        val claims = JwtClaimsSet.builder()
            .subject(userId.toString())
            .issuedAt(now)
            .expiresAt(now.plus(authProperties.accessTokenTtl))
            .claim("id", userId.toString())
            .claim("email", user.email)
            .claim("role", user.role.name)
            .build()
        val headers = JwsHeader.with(MacAlgorithm.HS256).build()
        return jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).tokenValue
    }
}
