package com.codechaser.essaygrading.llm

import com.codechaser.essaygrading.enums.GradingConfidence

data class GradingAiResponse(
    val totalScore: Int,
    val maxScore: Int,
    val rubricScores: List<RubricScore>,
    val deductions: List<Deduction>,
    val studentFeedback: String,
    val learningPoints: List<String>,
    val confidence: GradingConfidence,
    val reviewRequired: Boolean,
    val reviewReasons: List<String>,
    val modelName: String,
    val promptVersionName: String,
    val rawResponse: String,
) {
    data class RubricScore(
        val rubricItemName: String,
        val score: Int,
        val maxScore: Int,
        val reason: String,
    )

    data class Deduction(
        val rubricItemName: String,
        val pointsLost: Int,
        val reason: String,
    )
}
