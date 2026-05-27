package com.codechaser.essaygrading.api.grading

import com.codechaser.essaygrading.enums.GradingConfidence
import com.codechaser.essaygrading.enums.GradingStatus
import java.time.LocalDateTime

class Response {
    data class CreateGradingRequestData(
        val gradingRequestId: Long,
        val gradingResultId: Long?,
        val status: GradingStatus,
        val totalScore: Int?,
        val reviewRequired: Boolean?,
    )

    data class GradingResultData(
        val id: Long,
        val gradingRequestId: Long,
        val questionId: Long,
        val studentAnswer: String,
        val modelName: String,
        val promptVersionName: String,
        val totalScore: Int,
        val maxScore: Int,
        val confidence: GradingConfidence,
        val reviewRequired: Boolean,
        val rubricScores: List<RubricScoreData>,
        val deductions: List<DeductionData>,
        val studentFeedback: String,
        val learningPoints: List<String>,
        val reviewReasons: List<String>,
        val createdAt: LocalDateTime,
    )

    data class RubricScoreData(
        val rubricItemName: String,
        val score: Int,
        val maxScore: Int,
        val reason: String,
    )

    data class DeductionData(
        val rubricItemName: String,
        val pointsLost: Int,
        val reason: String,
    )
}
