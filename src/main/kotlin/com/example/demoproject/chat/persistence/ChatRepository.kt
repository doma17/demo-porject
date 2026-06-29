package com.example.demoproject.chat.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface ChatRepository : JpaRepository<ChatEntity, UUID> {
    fun findByThreadIdOrderByCreatedAtAsc(threadId: UUID): List<ChatEntity>

    @Query("select c from ChatEntity c where c.thread.id in :threadIds order by c.createdAt asc")
    fun findByThreadIdsOrderByCreatedAtAsc(@Param("threadIds") threadIds: Collection<UUID>): List<ChatEntity>

    fun countByCreatedAtGreaterThanEqual(createdAt: Instant): Long
}
