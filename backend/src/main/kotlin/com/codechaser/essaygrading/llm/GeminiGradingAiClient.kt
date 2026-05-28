package com.codechaser.essaygrading.llm

import com.codechaser.essaygrading.enums.GradingConfidence
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

@Component
@ConditionalOnProperty(name = ["llm.provider"], havingValue = "gemini")
class GeminiGradingAiClient(
    private val objectMapper: ObjectMapper,
    restClientBuilder: RestClient.Builder,
    @Value("\${gemini.api-key:}") private val apiKey: String,
    @Value("\${gemini.model:gemini-2.5-flash}") private val model: String,
    @Value("\${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") baseUrl: String,
) : GradingAiClient {
    private val restClient: RestClient = restClientBuilder.baseUrl(baseUrl).build()

    override fun grade(request: GradingAiRequest): GradingAiResponse {
        check(apiKey.isNotBlank()) {
            "GEMINI_API_KEY is required when LLM_PROVIDER=gemini."
        }

        val responseBody =
            try {
                restClient
                    .post()
                    .uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(buildGenerateContentRequest(request))
                    .retrieve()
                    .body(String::class.java)
            } catch (exception: RestClientResponseException) {
                val responseExcerpt = exception.responseBodyAsString.take(500)

                throw IllegalStateException(
                    "Gemini API call failed. status=${exception.statusCode.value()}, body=$responseExcerpt",
                    exception,
                )
            }

        val rawResponse = requireNotNull(responseBody) { "Gemini API response body is empty." }
        val gradingJson = extractModelText(rawResponse)
        val payload = objectMapper.readValue(gradingJson, GeminiGradingPayload::class.java)
        val normalizedRubricScores = normalizeRubricScores(request, payload)
        val normalizedDeductions = buildDeductions(normalizedRubricScores, payload)
        val missingRubricNames =
            request.rubricItems
                .map { it.name }
                .filterNot { rubricName -> payload.rubricScores.any { it.rubricItemName == rubricName } }
        val reviewReasons =
            (
                payload.reviewReasons +
                    missingRubricNames.map { "AI 응답에 '$it' 평가 항목 점수가 누락되어 수동 검토가 필요합니다." }
            ).distinct()
        val reviewRequired =
            payload.reviewRequired ||
                payload.confidence == GradingConfidence.LOW ||
                missingRubricNames.isNotEmpty()

        return GradingAiResponse(
            totalScore = normalizedRubricScores.sumOf { it.score },
            maxScore = request.totalScore,
            rubricScores = normalizedRubricScores,
            deductions = normalizedDeductions,
            studentFeedback = payload.studentFeedback,
            learningPoints = payload.learningPoints,
            confidence = payload.confidence,
            reviewRequired = reviewRequired,
            reviewReasons = reviewReasons,
            modelName = model,
            promptVersionName = PROMPT_VERSION_NAME,
            rawResponse = rawResponse,
        )
    }

    private fun normalizeRubricScores(
        request: GradingAiRequest,
        payload: GeminiGradingPayload,
    ): List<GradingAiResponse.RubricScore> {
        val scoresByName = payload.rubricScores.associateBy { it.rubricItemName }

        return request.rubricItems
            .sortedBy { it.sortOrder }
            .map { rubricItem ->
                val score = scoresByName[rubricItem.name]

                GradingAiResponse.RubricScore(
                    rubricItemName = rubricItem.name,
                    score = score?.score?.coerceIn(0, rubricItem.maxScore) ?: 0,
                    maxScore = rubricItem.maxScore,
                    reason =
                        score
                            ?.reason
                            ?.takeIf { it.isNotBlank() }
                            ?: "AI 응답에 해당 평가 항목의 채점 사유가 누락되어 수동 검토가 필요합니다.",
                )
            }
    }

    private fun buildDeductions(
        rubricScores: List<GradingAiResponse.RubricScore>,
        payload: GeminiGradingPayload,
    ): List<GradingAiResponse.Deduction> {
        val deductionsByRubricName = payload.deductions.associateBy { it.rubricItemName }

        return rubricScores
            .filter { it.score < it.maxScore }
            .map {
                val deductionReason =
                    deductionsByRubricName[it.rubricItemName]
                        ?.reason
                        ?.takeIf { reason -> reason.isNotBlank() }
                        ?: it.reason

                GradingAiResponse.Deduction(
                    rubricItemName = it.rubricItemName,
                    pointsLost = it.maxScore - it.score,
                    reason = deductionReason,
                )
            }
    }

    private fun buildGenerateContentRequest(request: GradingAiRequest): GeminiGenerateContentRequest =
        GeminiGenerateContentRequest(
            contents =
                listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = buildPrompt(request))),
                    ),
                ),
            generationConfig =
                GeminiGenerationConfig(
                    responseMimeType = "application/json",
                    responseSchema = gradingResponseSchema,
                ),
        )

    private fun buildPrompt(request: GradingAiRequest): String {
        val rubricText =
            request.rubricItems
                .sortedBy { it.sortOrder }
                .joinToString("\n") {
                    "- ${it.name}: ${it.criteria} (maxScore=${it.maxScore})"
                }

        return listOf(
            "You are an assistant that grades Korean student essay answers.",
            "Grade strictly using the question, model answer, and rubric.",
            "Return only valid JSON that matches the provided schema.",
            "",
            "Rules:",
            "- totalScore must equal the sum of rubricScores.score.",
            "- maxScore must be ${request.totalScore}.",
            "- Each rubric item must appear exactly once in rubricScores.",
            "- Each rubricScores.maxScore must match the rubric maxScore.",
            "- deductions must use only the rubric item names listed in the rubric.",
            "- confidence must be one of HIGH, MEDIUM, LOW.",
            "- If confidence is LOW, reviewRequired must be true.",
            "- Write feedback, reasons, learning points, and review reasons in Korean.",
            "",
            "Question title:",
            request.questionTitle,
            "",
            "Question:",
            request.questionContent,
            "",
            "Model answer:",
            request.modelAnswer,
            "",
            "Student answer:",
            request.studentAnswer,
            "",
            "Total score:",
            request.totalScore.toString(),
            "",
            "Rubric:",
            rubricText,
        ).joinToString("\n")
    }

    private fun extractModelText(rawResponse: String): String {
        val root = objectMapper.readTree(rawResponse)
        val generatedText = root.at("/candidates/0/content/parts/0/text").asText(null)

        return if (generatedText.isNullOrBlank()) {
            rawResponse
        } else {
            generatedText.trim()
        }
    }

    private data class GeminiGenerateContentRequest(
        val contents: List<GeminiContent>,
        val generationConfig: GeminiGenerationConfig,
    )

    private data class GeminiContent(
        val parts: List<GeminiPart>,
    )

    private data class GeminiPart(
        val text: String,
    )

    private data class GeminiGenerationConfig(
        val responseMimeType: String,
        val responseSchema: Map<String, Any>,
    )

    private data class GeminiGradingPayload(
        val totalScore: Int,
        val maxScore: Int,
        val rubricScores: List<GeminiRubricScorePayload>,
        val deductions: List<GeminiDeductionPayload>,
        val studentFeedback: String,
        val learningPoints: List<String>,
        val confidence: GradingConfidence,
        val reviewRequired: Boolean,
        val reviewReasons: List<String>,
    )

    private data class GeminiRubricScorePayload(
        val rubricItemName: String,
        val score: Int,
        val maxScore: Int,
        val reason: String,
    )

    private data class GeminiDeductionPayload(
        val rubricItemName: String,
        val pointsLost: Int,
        val reason: String,
    )

    companion object {
        private const val PROMPT_VERSION_NAME = "gemini-grading-v1"

        private val stringSchema = mapOf("type" to "string")
        private val integerSchema = mapOf("type" to "integer")
        private val booleanSchema = mapOf("type" to "boolean")
        private val stringArraySchema =
            mapOf(
                "type" to "array",
                "items" to stringSchema,
            )

        private val gradingResponseSchema: Map<String, Any> =
            mapOf(
                "type" to "object",
                "properties" to
                    mapOf(
                        "totalScore" to integerSchema,
                        "maxScore" to integerSchema,
                        "rubricScores" to
                            mapOf(
                                "type" to "array",
                                "items" to
                                    mapOf(
                                        "type" to "object",
                                        "properties" to
                                            mapOf(
                                                "rubricItemName" to stringSchema,
                                                "score" to integerSchema,
                                                "maxScore" to integerSchema,
                                                "reason" to stringSchema,
                                            ),
                                        "required" to listOf("rubricItemName", "score", "maxScore", "reason"),
                                    ),
                            ),
                        "deductions" to
                            mapOf(
                                "type" to "array",
                                "items" to
                                    mapOf(
                                        "type" to "object",
                                        "properties" to
                                            mapOf(
                                                "rubricItemName" to stringSchema,
                                                "pointsLost" to integerSchema,
                                                "reason" to stringSchema,
                                            ),
                                        "required" to listOf("rubricItemName", "pointsLost", "reason"),
                                    ),
                            ),
                        "studentFeedback" to stringSchema,
                        "learningPoints" to stringArraySchema,
                        "confidence" to mapOf("type" to "string", "enum" to GradingConfidence.entries.map { it.name }),
                        "reviewRequired" to booleanSchema,
                        "reviewReasons" to stringArraySchema,
                    ),
                "required" to
                    listOf(
                        "totalScore",
                        "maxScore",
                        "rubricScores",
                        "deductions",
                        "studentFeedback",
                        "learningPoints",
                        "confidence",
                        "reviewRequired",
                        "reviewReasons",
                    ),
            )
    }
}
