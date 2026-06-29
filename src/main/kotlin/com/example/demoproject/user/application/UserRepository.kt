package com.example.demoproject.user.application

import com.example.demoproject.user.domain.User

interface UserRepository {
	fun existsByEmail(email: String): Boolean
	fun save(user: User): User
}
