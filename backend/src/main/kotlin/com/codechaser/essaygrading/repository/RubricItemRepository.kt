package com.codechaser.essaygrading.repository

import com.codechaser.essaygrading.entity.RubricItemEntity
import org.springframework.data.jpa.repository.JpaRepository

interface RubricItemRepository : JpaRepository<RubricItemEntity, Long>
