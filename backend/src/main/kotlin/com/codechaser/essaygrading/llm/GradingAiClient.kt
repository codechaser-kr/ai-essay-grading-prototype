package com.codechaser.essaygrading.llm

interface GradingAiClient {
    fun grade(request: GradingAiRequest): GradingAiResponse
}
