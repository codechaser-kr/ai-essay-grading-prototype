package com.codechaser.essaygrading.common.error

class NotFoundException(
    override val message: String,
    val developerMessage: String? = null,
) : RuntimeException(message)
