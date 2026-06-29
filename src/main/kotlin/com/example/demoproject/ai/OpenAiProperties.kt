package com.example.demoproject.ai

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "ai.openai")
data class OpenAiProperties(
    val apiKey: String = "",
    val model: String = "gpt-5.5",
    val baseUrl: String = "https://api.openai.com/v1",
    val timeout: Duration = Duration.ofSeconds(30),
)
