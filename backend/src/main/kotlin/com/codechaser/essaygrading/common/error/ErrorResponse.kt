package com.codechaser.essaygrading.common.error

import java.time.LocalDateTime

data class ErrorResponse(
    val message: String,
    val developerMessage: String?,
    val status: Int,
    val path: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
)
