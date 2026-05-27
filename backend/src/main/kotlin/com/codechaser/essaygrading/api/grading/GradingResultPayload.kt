package com.codechaser.essaygrading.api.grading

import com.codechaser.essaygrading.enums.GradingConfidence

data class GradingResultPayload(
    val totalScore: Int,
    val maxScore: Int,
    val rubricScores: List<RubricScorePayload>,
    val deductions: List<DeductionPayload>,
    val studentFeedback: String,
    val learningPoints: List<String>,
    val confidence: GradingConfidence,
    val reviewRequired: Boolean,
    val reviewReasons: List<String>,
) {
    data class RubricScorePayload(
        val rubricItemName: String,
        val score: Int,
        val maxScore: Int,
        val reason: String,
    )

    data class DeductionPayload(
        val rubricItemName: String,
        val pointsLost: Int,
        val reason: String,
    )
}
