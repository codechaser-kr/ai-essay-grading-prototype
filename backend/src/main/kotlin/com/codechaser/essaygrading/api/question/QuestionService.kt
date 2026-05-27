package com.codechaser.essaygrading.api.question

import com.codechaser.essaygrading.common.error.NotFoundException
import com.codechaser.essaygrading.entity.QuestionEntity
import com.codechaser.essaygrading.entity.RubricItemEntity
import com.codechaser.essaygrading.repository.QuestionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class QuestionService(
    private val questionRepository: QuestionRepository,
) {
    @Transactional
    fun createQuestion(request: Request.CreateQuestionParams): Response.QuestionData {
        validateRubricTotalScore(request)

        val question =
            QuestionEntity(
                title = request.title.trim(),
                subject = request.subject.trim(),
                content = request.content.trim(),
                modelAnswer = request.modelAnswer.trim(),
                totalScore = request.totalScore,
            )

        request.rubricItems
            .sortedBy { it.sortOrder }
            .forEach {
                question.addRubricItem(
                    RubricItemEntity(
                        name = it.name.trim(),
                        criteria = it.criteria.trim(),
                        maxScore = it.maxScore,
                        sortOrder = it.sortOrder,
                    ),
                )
            }

        val savedQuestion = questionRepository.save(question)

        return Response.QuestionData(
            id = requireNotNull(savedQuestion.id),
            title = savedQuestion.title,
            subject = savedQuestion.subject,
            content = savedQuestion.content,
            modelAnswer = savedQuestion.modelAnswer,
            totalScore = savedQuestion.totalScore,
            rubricItems =
                savedQuestion.rubricItems
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
            createdAt = savedQuestion.createdAt,
            updatedAt = savedQuestion.updatedAt,
        )
    }

    @Transactional(readOnly = true)
    fun getQuestions(): List<Response.QuestionSummaryData> = questionRepository.findQuestionSummaries()

    @Transactional(readOnly = true)
    fun getQuestion(questionId: Long): Response.QuestionData =
        questionRepository
            .findQuestionDataById(questionId)
            ?: throw NotFoundException("문제를 찾을 수 없습니다.", "Question not found: id=$questionId")

    private fun validateRubricTotalScore(request: Request.CreateQuestionParams) {
        val rubricTotalScore = request.rubricItems.sumOf { it.maxScore }

        require(rubricTotalScore == request.totalScore) {
            "rubric maxScore 합계는 문제 총점과 일치해야 합니다. totalScore=${request.totalScore}, rubricTotalScore=$rubricTotalScore"
        }
    }
}
