package com.codechaser.essaygrading.api.grading

import com.codechaser.essaygrading.entity.QuestionEntity
import com.codechaser.essaygrading.entity.RubricItemEntity
import com.codechaser.essaygrading.enums.GradingConfidence
import com.codechaser.essaygrading.llm.GradingAiResponse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class GradingResultValidatorTest {
    private val validator = GradingResultValidator()

    @Test
    fun `rubric 점수 합계가 총점과 다르면 예외를 던진다`() {
        val response =
            validResponse().copy(
                totalScore = 90,
            )

        assertThrows(IllegalArgumentException::class.java) {
            validator.validate(question(), response)
        }
    }

    @Test
    fun `confidence가 LOW인데 reviewRequired가 false이면 예외를 던진다`() {
        val response =
            validResponse().copy(
                confidence = GradingConfidence.LOW,
                reviewRequired = false,
            )

        assertThrows(IllegalArgumentException::class.java) {
            validator.validate(question(), response)
        }
    }

    @Test
    fun `채점 최대 점수가 문제 총점과 다르면 예외를 던진다`() {
        val response =
            validResponse().copy(
                maxScore = 90,
            )

        assertThrows(IllegalArgumentException::class.java) {
            validator.validate(question(), response)
        }
    }

    @Test
    fun `등록되지 않은 rubric 항목의 감점 정보가 있으면 예외를 던진다`() {
        val response =
            validResponse().copy(
                deductions =
                    listOf(
                        GradingAiResponse.Deduction(
                            rubricItemName = "미등록 항목",
                            pointsLost = 3,
                            reason = "등록되지 않은 항목입니다.",
                        ),
                    ),
            )

        assertThrows(IllegalArgumentException::class.java) {
            validator.validate(question(), response)
        }
    }

    @Test
    fun `감점 점수가 음수이면 예외를 던진다`() {
        val response =
            validResponse().copy(
                deductions =
                    listOf(
                        GradingAiResponse.Deduction(
                            rubricItemName = "개념 이해",
                            pointsLost = -1,
                            reason = "음수 감점입니다.",
                        ),
                    ),
            )

        assertThrows(IllegalArgumentException::class.java) {
            validator.validate(question(), response)
        }
    }

    private fun question(): QuestionEntity {
        val question =
            QuestionEntity(
                title = "탄소 중립의 의미 설명",
                subject = "science",
                content = "탄소 중립이 무엇인지 설명하시오.",
                modelAnswer = "탄소 중립은 실질 배출량을 0으로 만드는 것이다.",
                totalScore = 100,
            )

        question.addRubricItem(
            RubricItemEntity(
                name = "개념 이해",
                criteria = "개념을 정확히 설명한다.",
                maxScore = 40,
                sortOrder = 1,
            ),
        )
        question.addRubricItem(
            RubricItemEntity(
                name = "실천 방안",
                criteria = "실천 방법을 제시한다.",
                maxScore = 40,
                sortOrder = 2,
            ),
        )
        question.addRubricItem(
            RubricItemEntity(
                name = "표현 명확성",
                criteria = "문장이 명확하다.",
                maxScore = 20,
                sortOrder = 3,
            ),
        )

        return question
    }

    private fun validResponse(): GradingAiResponse =
        GradingAiResponse(
            totalScore = 78,
            maxScore = 100,
            rubricScores =
                listOf(
                    GradingAiResponse.RubricScore(
                        rubricItemName = "개념 이해",
                        score = 32,
                        maxScore = 40,
                        reason = "핵심 개념을 일부 보완해야 합니다.",
                    ),
                    GradingAiResponse.RubricScore(
                        rubricItemName = "실천 방안",
                        score = 32,
                        maxScore = 40,
                        reason = "실천 방안을 더 구체화해야 합니다.",
                    ),
                    GradingAiResponse.RubricScore(
                        rubricItemName = "표현 명확성",
                        score = 14,
                        maxScore = 20,
                        reason = "표현을 더 명확히 다듬어야 합니다.",
                    ),
                ),
            deductions = emptyList(),
            studentFeedback = "핵심 개념을 보완하세요.",
            learningPoints = emptyList(),
            confidence = GradingConfidence.MEDIUM,
            reviewRequired = true,
            reviewReasons = listOf("핵심 개념 설명 확인이 필요합니다."),
            modelName = "mock-grading-model",
            promptVersionName = "mock-v1",
            rawResponse = "mock",
        )
}
