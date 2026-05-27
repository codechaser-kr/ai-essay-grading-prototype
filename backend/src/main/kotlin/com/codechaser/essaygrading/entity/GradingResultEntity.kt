package com.codechaser.essaygrading.entity

import com.codechaser.essaygrading.enums.GradingConfidence
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "grading_results")
class GradingResultEntity(
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grading_request_id", nullable = false, unique = true)
    var gradingRequest: GradingRequestEntity,
    @Column(nullable = false, length = 100)
    var modelName: String,
    @Column(nullable = false)
    var totalScore: Int,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var confidence: GradingConfidence,
    @Column(nullable = false)
    var reviewRequired: Boolean,
    @Column(nullable = false, columnDefinition = "text")
    var resultJson: String,
    @Column(nullable = false, columnDefinition = "text")
    var rawResponse: String,
    @Column(nullable = false, length = 100)
    var promptVersionName: String = "mock-v1",
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    lateinit var createdAt: LocalDateTime
}
