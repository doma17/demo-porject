package com.example.demoproject.auth.api

import com.example.demoproject.auth.application.AuthService
import com.example.demoproject.auth.application.LoginCommand
import com.example.demoproject.auth.application.LogoutCommand
import com.example.demoproject.auth.application.RefreshTokenCommand
import com.example.demoproject.auth.application.SignupCommand
import com.example.demoproject.common.api.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    fun signup(@Valid @RequestBody request: SignupRequest): ApiResponse<SignupResponse> {
        val result = authService.signup(SignupCommand(request.email, request.password, request.name))
        return ApiResponse.success(
            SignupResponse(
                id = result.id,
                email = result.email,
                name = result.name,
                role = result.role,
                createdAt = result.createdAt,
            ),
        )
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ApiResponse<TokenResponse> {
        val result = authService.login(LoginCommand(request.email, request.password))
        return ApiResponse.success(TokenResponse(result.accessToken, result.refreshToken))
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): ApiResponse<AccessTokenResponse> {
        val result = authService.refresh(RefreshTokenCommand(request.refreshToken))
        return ApiResponse.success(AccessTokenResponse(result.accessToken))
    }

    @PostMapping("/logout")
    fun logout(@Valid @RequestBody request: LogoutRequest): ApiResponse<Unit> {
        authService.logout(LogoutCommand(request.refreshToken))
        return ApiResponse.success()
    }
}
