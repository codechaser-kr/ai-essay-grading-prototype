package com.codechaser.essaygrading.api.grading

import com.codechaser.essaygrading.common.error.NotFoundException
import com.codechaser.essaygrading.entity.GradingRequestEntity
import com.codechaser.essaygrading.entity.GradingResultEntity
import com.codechaser.essaygrading.llm.GradingAiRequest
import com.codechaser.essaygrading.llm.GradingAiResponse
import com.codechaser.essaygrading.repository.GradingRequestRepository
import com.codechaser.essaygrading.repository.GradingResultRepository
import com.codechaser.essaygrading.repository.QuestionRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GradingRequestCommandService(
    private val questionRepository: QuestionRepository,
    private val gradingRequestRepository: GradingRequestRepository,
    private val gradingResultRepository: GradingResultRepository,
    private val gradingResultValidator: GradingResultValidator,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun createPendingRequest(request: Request.CreateGradingRequestParams): PendingGradingRequest {
        val questionEntity =
            questionRepository.findByIdOrNull(request.questionId)
                ?: throw NotFoundException(
                    "문제를 찾을 수 없습니다.",
                    "Question not found: id=${request.questionId}",
                )
        val gradingRequestEntity =
            gradingRequestRepository.save(
                GradingRequestEntity(
                    question = questionEntity,
                    studentAnswer = request.studentAnswer.trim(),
                ),
            )

        return PendingGradingRequest(
            gradingRequestId = requireNotNull(gradingRequestEntity.id),
            aiRequest =
                GradingAiRequest(
                    questionId = requireNotNull(questionEntity.id),
                    questionTitle = questionEntity.title,
                    questionContent = questionEntity.content,
                    modelAnswer = questionEntity.modelAnswer,
                    studentAnswer = gradingRequestEntity.studentAnswer,
                    totalScore = questionEntity.totalScore,
                    rubricItems =
                        questionEntity.rubricItems
                            .sortedBy { it.sortOrder }
                            .map {
                                GradingAiRequest.RubricItem(
                                    name = it.name,
                                    criteria = it.criteria,
                                    maxScore = it.maxScore,
                                    sortOrder = it.sortOrder,
                                )
                            },
                ),
        )
    }

    @Transactional
    fun markProcessing(gradingRequestId: Long) {
        val gradingRequestEntity = getGradingRequest(gradingRequestId)

        gradingRequestEntity.markProcessing()
    }

    @Transactional
    fun complete(
        gradingRequestId: Long,
        aiResponse: GradingAiResponse,
    ): Response.CreateGradingRequestData {
        val gradingRequestEntity = getGradingRequest(gradingRequestId)

        gradingResultValidator.validate(gradingRequestEntity.question, aiResponse)

        val payload =
            GradingResultPayload(
                totalScore = aiResponse.totalScore,
                maxScore = aiResponse.maxScore,
                rubricScores =
                    aiResponse.rubricScores.map {
                        GradingResultPayload.RubricScorePayload(
                            rubricItemName = it.rubricItemName,
                            score = it.score,
                            maxScore = it.maxScore,
                            reason = it.reason,
                        )
                    },
                deductions =
                    aiResponse.deductions.map {
                        GradingResultPayload.DeductionPayload(
                            rubricItemName = it.rubricItemName,
                            pointsLost = it.pointsLost,
                            reason = it.reason,
                        )
                    },
                studentFeedback = aiResponse.studentFeedback,
                learningPoints = aiResponse.learningPoints,
                confidence = aiResponse.confidence,
                reviewRequired = aiResponse.reviewRequired,
                reviewReasons = aiResponse.reviewReasons,
            )
        val gradingResult =
            gradingResultRepository.save(
                GradingResultEntity(
                    gradingRequest = gradingRequestEntity,
                    modelName = aiResponse.modelName,
                    totalScore = aiResponse.totalScore,
                    confidence = aiResponse.confidence,
                    reviewRequired = aiResponse.reviewRequired,
                    resultJson = objectMapper.writeValueAsString(payload),
                    rawResponse = aiResponse.rawResponse,
                    promptVersionName = aiResponse.promptVersionName,
                ),
            )

        gradingRequestEntity.markCompleted()

        return Response.CreateGradingRequestData(
            gradingRequestId = requireNotNull(gradingRequestEntity.id),
            gradingResultId = requireNotNull(gradingResult.id),
            status = gradingRequestEntity.status,
            totalScore = gradingResult.totalScore,
            reviewRequired = gradingResult.reviewRequired,
        )
    }

    @Transactional
    fun fail(
        gradingRequestId: Long,
        message: String,
    ) {
        val gradingRequestEntity = getGradingRequest(gradingRequestId)

        gradingRequestEntity.markFailed(message)
    }

    private fun getGradingRequest(gradingRequestId: Long): GradingRequestEntity =
        gradingRequestRepository.findByIdOrNull(gradingRequestId)
            ?: throw NotFoundException(
                "채점 요청을 찾을 수 없습니다.",
                "Grading request not found: id=$gradingRequestId",
            )
}

data class PendingGradingRequest(
    val gradingRequestId: Long,
    val aiRequest: GradingAiRequest,
)
