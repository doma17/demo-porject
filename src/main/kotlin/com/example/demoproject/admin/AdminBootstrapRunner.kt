package com.example.demoproject.admin

import com.example.demoproject.user.persistence.UserEntity
import com.example.demoproject.user.persistence.UserRepository
import com.example.demoproject.user.persistence.UserRole
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Component
class AdminBootstrapRunner(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val clock: Clock,
    @Value("\${admin.bootstrap.email:}") private val email: String,
    @Value("\${admin.bootstrap.password:}") private val password: String,
    @Value("\${admin.bootstrap.name:Admin}") private val name: String,
) : ApplicationRunner {
    @Transactional
    override fun run(args: ApplicationArguments?) {
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isBlank() || password.isBlank() || userRepository.existsByEmail(normalizedEmail)) return

        userRepository.save(
            UserEntity(
                email = normalizedEmail,
                passwordHash = passwordEncoder.encode(password),
                name = name.trim().ifBlank { "Admin" },
                role = UserRole.admin,
                createdAt = Instant.now(clock),
            ),
        )
    }
}
