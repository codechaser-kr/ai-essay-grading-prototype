package com.codechaser.essaygrading.api.grading

import com.codechaser.essaygrading.common.error.NotFoundException
import com.codechaser.essaygrading.llm.GradingAiClient
import com.codechaser.essaygrading.repository.GradingResultRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GradingService(
    private val gradingResultRepository: GradingResultRepository,
    private val gradingRequestCommandService: GradingRequestCommandService,
    private val gradingAiClient: GradingAiClient,
    private val objectMapper: ObjectMapper,
) {
    fun createGradingRequest(request: Request.CreateGradingRequestParams): Response.CreateGradingRequestData {
        val pendingRequest = gradingRequestCommandService.createPendingRequest(request)
        gradingRequestCommandService.markProcessing(pendingRequest.gradingRequestId)

        try {
            val aiResponse = gradingAiClient.grade(pendingRequest.aiRequest)
            return gradingRequestCommandService.complete(pendingRequest.gradingRequestId, aiResponse)
        } catch (exception: Exception) {
            gradingRequestCommandService.fail(
                pendingRequest.gradingRequestId,
                exception.message ?: "채점 처리에 실패했습니다.",
            )
            throw exception
        }
    }

    @Transactional(readOnly = true)
    fun getGradingResult(gradingResultId: Long): Response.GradingResultData {
        val gradingResultEntity =
            gradingResultRepository.findByIdOrNull(gradingResultId)
                ?: throw NotFoundException(
                    "채점 결과를 찾을 수 없습니다.",
                    "Grading result not found: id=$gradingResultId",
                )
        val payload = objectMapper.readValue(gradingResultEntity.resultJson, GradingResultPayload::class.java)

        return Response.GradingResultData(
            id = requireNotNull(gradingResultEntity.id),
            gradingRequestId = requireNotNull(gradingResultEntity.gradingRequest.id),
            questionId = requireNotNull(gradingResultEntity.gradingRequest.question.id),
            studentAnswer = gradingResultEntity.gradingRequest.studentAnswer,
            modelName = gradingResultEntity.modelName,
            promptVersionName = gradingResultEntity.promptVersionName,
            totalScore = gradingResultEntity.totalScore,
            maxScore = payload.maxScore,
            confidence = gradingResultEntity.confidence,
            reviewRequired = gradingResultEntity.reviewRequired,
            rubricScores =
                payload.rubricScores.map {
                    Response.RubricScoreData(
                        rubricItemName = it.rubricItemName,
                        score = it.score,
                        maxScore = it.maxScore,
                        reason = it.reason,
                    )
                },
            deductions =
                payload.deductions.map {
                    Response.DeductionData(
                        rubricItemName = it.rubricItemName,
                        pointsLost = it.pointsLost,
                        reason = it.reason,
                    )
                },
            studentFeedback = payload.studentFeedback,
            learningPoints = payload.learningPoints,
            reviewReasons = payload.reviewReasons,
            createdAt = gradingResultEntity.createdAt,
        )
    }

    @Transactional(readOnly = true)
    fun getGradingResults(questionId: Long): List<Response.GradingResultData> =
        gradingResultRepository
            .findAllByGradingRequestQuestionIdOrderByCreatedAtDesc(questionId)
            .map { gradingResultEntity ->
                val payload = objectMapper.readValue(gradingResultEntity.resultJson, GradingResultPayload::class.java)

                Response.GradingResultData(
                    id = requireNotNull(gradingResultEntity.id),
                    gradingRequestId = requireNotNull(gradingResultEntity.gradingRequest.id),
                    questionId = requireNotNull(gradingResultEntity.gradingRequest.question.id),
                    studentAnswer = gradingResultEntity.gradingRequest.studentAnswer,
                    modelName = gradingResultEntity.modelName,
                    promptVersionName = gradingResultEntity.promptVersionName,
                    totalScore = gradingResultEntity.totalScore,
                    maxScore = payload.maxScore,
                    confidence = gradingResultEntity.confidence,
                    reviewRequired = gradingResultEntity.reviewRequired,
                    rubricScores =
                        payload.rubricScores.map {
                            Response.RubricScoreData(
                                rubricItemName = it.rubricItemName,
                                score = it.score,
                                maxScore = it.maxScore,
                                reason = it.reason,
                            )
                        },
                    deductions =
                        payload.deductions.map {
                            Response.DeductionData(
                                rubricItemName = it.rubricItemName,
                                pointsLost = it.pointsLost,
                                reason = it.reason,
                            )
                        },
                    studentFeedback = payload.studentFeedback,
                    learningPoints = payload.learningPoints,
                    reviewReasons = payload.reviewReasons,
                    createdAt = gradingResultEntity.createdAt,
                )
            }
}
