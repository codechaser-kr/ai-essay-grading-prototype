package com.codechaser.essaygrading.api.grading

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

class Request {
    data class CreateGradingRequestParams(
        @field:NotNull
        val questionId: Long,
        @field:NotBlank
        @field:Size(max = 10000, message = "답안은 최대 10,000자까지 입력 가능합니다.")
        val studentAnswer: String,
    )
}
