package com.codechaser.essaygrading.api.question

import java.time.LocalDateTime

class Response {
    data class QuestionSummaryData(
        val id: Long,
        val title: String,
        val subject: String,
        val totalScore: Int,
        val rubricItemCount: Long,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
    )

    data class QuestionData(
        val id: Long,
        val title: String,
        val subject: String,
        val content: String,
        val modelAnswer: String,
        val totalScore: Int,
        val rubricItems: List<RubricItemData>,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
    )

    data class RubricItemData(
        val id: Long,
        val name: String,
        val criteria: String,
        val maxScore: Int,
        val sortOrder: Int,
    )
}
