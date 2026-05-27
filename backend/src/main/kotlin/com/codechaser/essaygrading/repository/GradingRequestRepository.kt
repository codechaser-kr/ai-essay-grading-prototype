package com.codechaser.essaygrading.repository

import com.codechaser.essaygrading.entity.GradingRequestEntity
import org.springframework.data.jpa.repository.JpaRepository

interface GradingRequestRepository : JpaRepository<GradingRequestEntity, Long>
