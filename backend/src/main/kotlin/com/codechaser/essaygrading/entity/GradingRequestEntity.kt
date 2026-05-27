package com.codechaser.essaygrading.entity

import com.codechaser.essaygrading.enums.GradingStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "grading_requests")
class GradingRequestEntity(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    var question: QuestionEntity,
    @Column(nullable = false, columnDefinition = "text")
    var studentAnswer: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: GradingStatus = GradingStatus.PENDING

    @Column(columnDefinition = "text")
    var errorMessage: String? = null

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    lateinit var createdAt: LocalDateTime

    fun markProcessing() {
        status = GradingStatus.PROCESSING
        errorMessage = null
    }

    fun markCompleted() {
        status = GradingStatus.COMPLETED
        errorMessage = null
    }

    fun markFailed(message: String) {
        status = GradingStatus.FAILED
        errorMessage = message
    }
}
