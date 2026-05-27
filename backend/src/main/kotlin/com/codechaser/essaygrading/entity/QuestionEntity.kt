package com.codechaser.essaygrading.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "questions")
class QuestionEntity(
    @Column(nullable = false, length = 200)
    var title: String,
    @Column(nullable = false, length = 100)
    var subject: String,
    @Column(nullable = false, columnDefinition = "text")
    var content: String,
    @Column(nullable = false, columnDefinition = "text")
    var modelAnswer: String,
    @Column(nullable = false)
    var totalScore: Int,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @OneToMany(
        mappedBy = "question",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY,
    )
    var rubricItems: MutableList<RubricItemEntity> = mutableListOf()
        protected set

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    lateinit var createdAt: LocalDateTime

    @UpdateTimestamp
    @Column(nullable = false)
    lateinit var updatedAt: LocalDateTime

    fun addRubricItem(rubricItem: RubricItemEntity) {
        rubricItems.add(rubricItem)
        rubricItem.question = this
    }
}
