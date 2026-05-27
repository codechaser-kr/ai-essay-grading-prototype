package com.codechaser.essaygrading.api.grading

import com.codechaser.essaygrading.common.error.NotFoundException
import com.codechaser.essaygrading.entity.GradingRequestEntity
import com.codechaser.essaygrading.entity.GradingResultEntity
import com.codechaser.essaygrading.llm.GradingAiClient
import com.codechaser.essaygrading.llm.GradingAiRequest
import com.codechaser.essaygrading.repository.GradingRequestRepository
import com.codechaser.essaygrading.repository.GradingResultRepository
import com.codechaser.essaygrading.repository.QuestionRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GradingService(
    private val questionRepository: QuestionRepository,
    private val gradingRequestRepository: GradingRequestRepository,
    private val gradingResultRepository: GradingResultRepository,
    private val gradingAiClient: GradingAiClient,
    private val gradingResultValidator: GradingResultValidator,
    private val objectMapper: ObjectMapper,
) {
    @Transactional(noRollbackFor = [IllegalArgumentException::class])
    fun createGradingRequest(request: Request.CreateGradingRequestParams): Response.CreateGradingRequestData {
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

        try {
            gradingRequestEntity.markProcessing()

            val aiResponse =
                gradingAiClient.grade(
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
            gradingResultValidator.validate(questionEntity, aiResponse)

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
        } catch (exception: IllegalArgumentException) {
            gradingRequestEntity.markFailed(exception.message ?: "채점 결과 검증에 실패했습니다.")
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
