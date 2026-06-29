package com.example.demoproject.user.api

import com.example.demoproject.common.api.ApiResponse
import com.example.demoproject.user.application.CurrentUserService
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/users")
class UserController(
    private val currentUserService: CurrentUserService,
) {
    @GetMapping("/me")
    fun me(@AuthenticationPrincipal jwt: Jwt): ApiResponse<CurrentUserResponse> {
        val user = currentUserService.getCurrentUser(UUID.fromString(jwt.subject))
        return ApiResponse.success(
            CurrentUserResponse(
                id = user.id,
                email = user.email,
                name = user.name,
                role = user.role,
                createdAt = user.createdAt,
            ),
        )
    }
}

data class CurrentUserResponse(
    val id: UUID,
    val email: String,
    val name: String,
    val role: String,
    val createdAt: Instant,
)
