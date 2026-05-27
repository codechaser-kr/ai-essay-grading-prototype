package com.codechaser.essaygrading.api.grading

import com.codechaser.essaygrading.entity.QuestionEntity
import com.codechaser.essaygrading.enums.GradingConfidence
import com.codechaser.essaygrading.llm.GradingAiResponse
import org.springframework.stereotype.Component

@Component
class GradingResultValidator {
    fun validate(
        question: QuestionEntity,
        response: GradingAiResponse,
    ) {
        require(response.totalScore >= 0) {
            "채점 총점은 0 이상이어야 합니다. totalScore=${response.totalScore}"
        }
        require(response.totalScore <= question.totalScore) {
            "채점 총점은 문제 총점을 초과할 수 없습니다. totalScore=${response.totalScore}, questionTotalScore=${question.totalScore}"
        }
        require(response.rubricScores.sumOf { it.score } == response.totalScore) {
            "rubricScores 점수 합계는 채점 총점과 일치해야 합니다."
        }
        require(response.rubricScores.size == question.rubricItems.size) {
            "rubricScores 개수는 등록된 rubricItems 개수와 일치해야 합니다."
        }

        val rubricItemsByName = question.rubricItems.associateBy { it.name }
        response.rubricScores.forEach { score ->
            val rubricItem =
                requireNotNull(rubricItemsByName[score.rubricItemName]) {
                    "등록되지 않은 rubric 항목의 점수가 포함되어 있습니다. rubricItemName=${score.rubricItemName}"
                }

            require(score.score in 0..score.maxScore) {
                "rubric score는 0 이상 maxScore 이하여야 합니다. rubricItemName=${score.rubricItemName}"
            }
            require(score.maxScore == rubricItem.maxScore) {
                "rubric score의 maxScore는 등록된 rubric maxScore와 일치해야 합니다. rubricItemName=${score.rubricItemName}"
            }
        }

        require(response.confidence != GradingConfidence.LOW || response.reviewRequired) {
            "confidence가 LOW이면 reviewRequired는 true여야 합니다."
        }
    }
}
