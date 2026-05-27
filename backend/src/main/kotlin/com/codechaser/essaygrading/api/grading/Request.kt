package com.codechaser.essaygrading.api.grading

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

class Request {
    data class CreateGradingRequestParams(
        @field:NotNull
        val questionId: Long,
        @field:NotBlank
        val studentAnswer: String,
    )
}
