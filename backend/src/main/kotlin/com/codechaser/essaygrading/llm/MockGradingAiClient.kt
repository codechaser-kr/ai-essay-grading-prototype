package com.codechaser.essaygrading.llm

import com.codechaser.essaygrading.enums.GradingConfidence
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["llm.provider"], havingValue = "mock", matchIfMissing = true)
class MockGradingAiClient : GradingAiClient {
    override fun grade(request: GradingAiRequest): GradingAiResponse {
        val rubricScores =
            request.rubricItems
                .sortedBy { it.sortOrder }
                .mapIndexed { index, rubric ->
                    val pointsLost =
                        when (index) {
                            0 -> minOf(8, rubric.maxScore)
                            1 -> minOf(8, rubric.maxScore)
                            else -> minOf(6, rubric.maxScore)
                        }
                    val score = rubric.maxScore - pointsLost

                    GradingAiResponse.RubricScore(
                        rubricItemName = rubric.name,
                        score = score,
                        maxScore = rubric.maxScore,
                        reason = "${rubric.name} 기준을 일부 충족했지만 보완할 부분이 있습니다.",
                    )
                }

        val deductions =
            rubricScores
                .filter { it.score < it.maxScore }
                .map {
                    GradingAiResponse.Deduction(
                        rubricItemName = it.rubricItemName,
                        pointsLost = it.maxScore - it.score,
                        reason = "${it.rubricItemName} 항목에서 설명의 정확성 또는 구체성이 부족합니다.",
                    )
                }

        val totalScore = rubricScores.sumOf { it.score }
        val reviewRequired = totalScore < request.totalScore * REVIEW_REQUIRED_THRESHOLD

        return GradingAiResponse(
            totalScore = totalScore,
            maxScore = request.totalScore,
            rubricScores = rubricScores,
            deductions = deductions,
            studentFeedback = "답변의 방향은 적절하지만 핵심 개념을 더 정확하고 구체적으로 설명할 필요가 있습니다.",
            learningPoints =
                listOf(
                    "문제의 핵심 개념을 모범 답안과 비교해 복습하세요.",
                    "평가 기준별로 빠진 내용을 한 문장씩 보완하는 연습을 하세요.",
                ),
            confidence = GradingConfidence.MEDIUM,
            reviewRequired = reviewRequired,
            reviewReasons =
                if (reviewRequired) {
                    listOf("핵심 개념 설명에 오개념 또는 누락 가능성이 있습니다.")
                } else {
                    emptyList()
                },
            modelName = "mock-grading-model",
            promptVersionName = "mock-v1",
            rawResponse = "mock response generated without external LLM API call",
        )
    }

    companion object {
        private const val REVIEW_REQUIRED_THRESHOLD = 0.8
    }
}
