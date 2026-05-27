package com.codechaser.essaygrading.repository

import com.codechaser.essaygrading.api.question.Response
import com.codechaser.essaygrading.entity.QuestionEntity
import com.codechaser.essaygrading.entity.RubricItemEntity
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import org.springframework.data.jpa.repository.JpaRepository

interface QuestionRepository :
    JpaRepository<QuestionEntity, Long>,
    CustomQuestionRepository

interface CustomQuestionRepository {
    fun findQuestionSummaries(): List<Response.QuestionSummaryData>

    fun findQuestionDataById(questionId: Long): Response.QuestionData?
}

class CustomQuestionRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
) : CustomQuestionRepository {
    override fun findQuestionSummaries(): List<Response.QuestionSummaryData> =
        kotlinJdslJpqlExecutor
            .findAll {
                selectNew<Response.QuestionSummaryData>(
                    path(QuestionEntity::id),
                    path(QuestionEntity::title),
                    path(QuestionEntity::subject),
                    path(QuestionEntity::totalScore),
                    count(path(RubricItemEntity::id)),
                    path(QuestionEntity::createdAt),
                    path(QuestionEntity::updatedAt),
                ).from(
                    entity(QuestionEntity::class),
                    leftJoin(QuestionEntity::rubricItems),
                ).groupBy(
                    path(QuestionEntity::id),
                    path(QuestionEntity::title),
                    path(QuestionEntity::subject),
                    path(QuestionEntity::totalScore),
                    path(QuestionEntity::createdAt),
                    path(QuestionEntity::updatedAt),
                ).orderBy(
                    path(QuestionEntity::createdAt).desc(),
                )
            }.filterNotNull()

    override fun findQuestionDataById(questionId: Long): Response.QuestionData? {
        val question =
            kotlinJdslJpqlExecutor
                .findAll(limit = 1) {
                    val questionEntity = entity(QuestionEntity::class)

                    select(questionEntity)
                        .from(
                            questionEntity,
                        ).where(
                            path(QuestionEntity::id).equal(questionId),
                        )
                }.firstOrNull()
                ?: return null

        return Response.QuestionData(
            id = requireNotNull(question.id),
            title = question.title,
            subject = question.subject,
            content = question.content,
            modelAnswer = question.modelAnswer,
            totalScore = question.totalScore,
            rubricItems =
                question.rubricItems
                    .sortedBy { it.sortOrder }
                    .map {
                        Response.RubricItemData(
                            id = requireNotNull(it.id),
                            name = it.name,
                            criteria = it.criteria,
                            maxScore = it.maxScore,
                            sortOrder = it.sortOrder,
                        )
                    },
            createdAt = question.createdAt,
            updatedAt = question.updatedAt,
        )
    }
}
