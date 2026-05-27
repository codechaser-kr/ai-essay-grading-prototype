package com.codechaser.essaygrading.llm

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["llm.provider"], havingValue = "openai")
class OpenAiGradingAiClient : GradingAiClient {
    override fun grade(request: GradingAiRequest): GradingAiResponse =
        throw UnsupportedOperationException("OpenAI grading provider is not implemented in MVP.")
}
