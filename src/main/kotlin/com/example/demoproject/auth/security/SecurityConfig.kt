package com.example.demoproject.auth.security

import com.example.demoproject.ai.OpenAiProperties
import com.example.demoproject.common.api.ApiResponse
import com.fasterxml.jackson.databind.ObjectMapper
import com.nimbusds.jose.jwk.source.ImmutableSecret
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.core.convert.converter.Converter
import java.time.Clock
import javax.crypto.spec.SecretKeySpec

@Configuration
@EnableConfigurationProperties(AuthProperties::class, OpenAiProperties::class)
class SecurityConfig {
    @Bean
    fun securityFilterChain(http: HttpSecurity, objectMapper: ObjectMapper): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/", "/index.html", "/favicon.ico", "/assets/**", "/*.css", "/*.js", "/error").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/signup", "/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint(apiAuthenticationEntryPoint(objectMapper))
            }
            .oauth2ResourceServer { resource ->
                resource.authenticationEntryPoint(apiAuthenticationEntryPoint(objectMapper))
                resource.jwt { jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()) }
            }
            .build()

    private fun apiAuthenticationEntryPoint(objectMapper: ObjectMapper): AuthenticationEntryPoint =
        AuthenticationEntryPoint { _, response, _ ->
            response.status = HttpStatus.UNAUTHORIZED.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            objectMapper.writeValue(response.writer, ApiResponse.error("UNAUTHORIZED", "Authentication is required"))
        }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun jwtEncoder(authProperties: AuthProperties): JwtEncoder =
        NimbusJwtEncoder(ImmutableSecret(authProperties.secret.toByteArray()))

    @Bean
    fun jwtDecoder(authProperties: AuthProperties): JwtDecoder {
        val key = SecretKeySpec(authProperties.secret.toByteArray(), "HmacSHA256")
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build()
    }

    @Bean
    fun clock(): Clock = Clock.systemUTC()

    private fun jwtAuthenticationConverter(): Converter<Jwt, AbstractAuthenticationToken> = Converter { jwt ->
        val role = jwt.getClaimAsString("role")
        val authorities = if (role.isNullOrBlank()) emptyList() else listOf(SimpleGrantedAuthority("ROLE_${role.uppercase()}"))
        JwtAuthenticationToken(jwt, authorities, jwt.subject)
    }
}
