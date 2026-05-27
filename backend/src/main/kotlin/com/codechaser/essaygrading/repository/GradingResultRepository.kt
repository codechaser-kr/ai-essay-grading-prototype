package com.codechaser.essaygrading.repository

import com.codechaser.essaygrading.entity.GradingResultEntity
import org.springframework.data.jpa.repository.JpaRepository

interface GradingResultRepository : JpaRepository<GradingResultEntity, Long> {
    fun findAllByGradingRequestQuestionIdOrderByCreatedAtDesc(questionId: Long): List<GradingResultEntity>
}
