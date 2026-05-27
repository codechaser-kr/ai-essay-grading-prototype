package com.codechaser.essaygrading.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "rubric_items")
class RubricItemEntity(
    @Column(nullable = false, length = 100)
    var name: String,
    @Column(nullable = false, columnDefinition = "text")
    var criteria: String,
    @Column(nullable = false)
    var maxScore: Int,
    @Column(nullable = false)
    var sortOrder: Int,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    lateinit var question: QuestionEntity
}
