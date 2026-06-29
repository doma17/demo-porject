package com.example.demoproject.auth.security

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "auth.jwt")
data class AuthProperties(
    val secret: String = "local-demo-secret-must-be-at-least-32-bytes-long",
    val accessTokenTtl: Duration = Duration.ofMinutes(15),
    val refreshTokenTtl: Duration = Duration.ofDays(7),
)
