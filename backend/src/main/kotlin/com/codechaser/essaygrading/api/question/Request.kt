package com.codechaser.essaygrading.api.question

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

class Request {
    data class CreateQuestionParams(
        @field:NotBlank
        @field:Size(max = 200)
        val title: String,
        @field:NotBlank
        @field:Size(max = 100)
        val subject: String,
        @field:NotBlank
        val content: String,
        @field:NotBlank
        val modelAnswer: String,
        @field:Min(1)
        val totalScore: Int,
        @field:Valid
        @field:Size(min = 1)
        val rubricItems: List<CreateRubricItemParams>,
    )

    data class CreateRubricItemParams(
        @field:NotBlank
        @field:Size(max = 100)
        val name: String,
        @field:NotBlank
        val criteria: String,
        @field:Min(1)
        val maxScore: Int,
        @field:Min(1)
        val sortOrder: Int,
    )
}
