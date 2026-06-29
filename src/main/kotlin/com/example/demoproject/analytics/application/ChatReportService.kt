package com.example.demoproject.analytics.application

import com.example.demoproject.analytics.persistence.ChatReportEntryEntity
import com.example.demoproject.analytics.persistence.ChatReportEntryRepository
import com.example.demoproject.user.persistence.UserEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.Instant

@Service
class ChatReportService(
    private val chatReportEntryRepository: ChatReportEntryRepository,
    private val activityLogRecorder: ActivityLogRecorder,
    private val clock: Clock,
) {
    @Transactional
    fun recordChat(user: UserEntity, question: String, answer: String): ChatReportEntryEntity {
        activityLogRecorder.recordChatCreated()
        return chatReportEntryRepository.save(
            ChatReportEntryEntity(
                user = user,
                question = question,
                answer = answer,
                createdAt = Instant.now(clock),
            ),
        )
    }

    @Transactional(readOnly = true)
    fun generateLastDayCsv(): String {
        val until = Instant.now(clock)
        val since = until.minus(Duration.ofDays(1))
        val rows = chatReportEntryRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(since, until)
        return buildString {
            appendLine("createdAt,userId,email,name,question,answer")
            rows.forEach { row ->
                appendLine(
                    listOf(
                        row.createdAt.toString(),
                        requireNotNull(row.user.id).toString(),
                        row.user.email,
                        row.user.name,
                        row.question,
                        row.answer,
                    ).joinToString(",") { csvCell(it) },
                )
            }
        }
    }

    private fun csvCell(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }
}
