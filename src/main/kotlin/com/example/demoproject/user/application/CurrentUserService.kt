package com.example.demoproject.user.application

import com.example.demoproject.common.error.InvalidCredentialsException
import com.example.demoproject.user.persistence.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class CurrentUserService(
    private val userRepository: UserRepository,
) {
    @Transactional(readOnly = true)
    fun getCurrentUser(userId: UUID): CurrentUserResult {
        val user = userRepository.findById(userId).orElseThrow { InvalidCredentialsException() }
        return CurrentUserResult(
            id = requireNotNull(user.id),
            email = user.email,
            name = user.name,
            role = user.role.name,
            createdAt = user.createdAt,
        )
    }
}

data class CurrentUserResult(
    val id: UUID,
    val email: String,
    val name: String,
    val role: String,
    val createdAt: Instant,
)
