package com.example.demoproject.chat.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional
import java.util.UUID

interface ChatThreadRepository : JpaRepository<ChatThreadEntity, UUID> {
    @Query("""
        select t from ChatThreadEntity t
        where t.user.id = :userId and t.deletedAt is null
        order by t.lastChatAt desc
    """)
    fun findLatestActiveByUserId(@Param("userId") userId: UUID, pageable: Pageable): List<ChatThreadEntity>

    @Query("select t from ChatThreadEntity t where t.user.id = :userId and t.deletedAt is null")
    fun findActiveByUserId(@Param("userId") userId: UUID, pageable: Pageable): Page<ChatThreadEntity>

    @Query("select t from ChatThreadEntity t where t.deletedAt is null")
    fun findAllActive(pageable: Pageable): Page<ChatThreadEntity>

    @Query("select t from ChatThreadEntity t where t.id = :threadId and t.deletedAt is null")
    fun findActiveById(@Param("threadId") threadId: UUID): Optional<ChatThreadEntity>
}
