package com.codechaser.essaygrading.llm

data class GradingAiRequest(
    val questionId: Long,
    val questionTitle: String,
    val questionContent: String,
    val modelAnswer: String,
    val studentAnswer: String,
    val totalScore: Int,
    val rubricItems: List<RubricItem>,
) {
    data class RubricItem(
        val name: String,
        val criteria: String,
        val maxScore: Int,
        val sortOrder: Int,
    )
}
