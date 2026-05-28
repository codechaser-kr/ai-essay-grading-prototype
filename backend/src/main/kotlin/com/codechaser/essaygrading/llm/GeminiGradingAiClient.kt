package com.codechaser.essaygrading.llm

import com.codechaser.essaygrading.enums.GradingConfidence
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.web.client.ClientHttpRequestFactories
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.time.Duration

@Component
@ConditionalOnProperty(name = ["llm.provider"], havingValue = "gemini")
class GeminiGradingAiClient(
    private val objectMapper: ObjectMapper,
    private val restClient: RestClient,
    private val apiKey: String,
    private val model: String,
) : GradingAiClient {
    @Autowired
    constructor(
        objectMapper: ObjectMapper,
        restClientBuilder: RestClient.Builder,
        @Value("\${gemini.api-key:}") apiKey: String,
        @Value("\${gemini.model:gemini-2.5-flash}") model: String,
        @Value("\${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") baseUrl: String,
        @Value("\${gemini.timeout-seconds:60}") timeoutSeconds: Long,
    ) : this(
        objectMapper = objectMapper,
        restClient = buildRestClient(restClientBuilder, baseUrl, timeoutSeconds),
        apiKey = apiKey,
        model = model,
    )

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
        val deductionReasonsByRubricName =
            payload.deductions
                .groupBy { it.rubricItemName }
                .mapValues { (_, deductions) ->
                    deductions
                        .map { it.reason.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .joinToString(" / ")
                }

        return rubricScores
            .filter { it.score < it.maxScore }
            .map {
                val deductionReason =
                    deductionReasonsByRubricName[it.rubricItemName]
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
            "당신은 한국어 서술형 답안을 채점하는 보조 교사입니다.",
            "문제, 모범 답안, 평가 기준을 기준으로 학생 답안을 엄격하게 채점하세요.",
            "반드시 제공된 JSON schema에 맞는 JSON만 반환하세요.",
            "",
            "채점 규칙:",
            "- totalScore는 rubricScores.score 합계와 정확히 일치해야 합니다.",
            "- maxScore는 ${request.totalScore}이어야 합니다.",
            "- 모든 평가 항목은 rubricScores에 정확히 한 번씩 포함되어야 합니다.",
            "- 각 rubricScores.maxScore는 제공된 평가 항목의 maxScore와 일치해야 합니다.",
            "- deductions에는 제공된 평가 항목명만 사용해야 합니다.",
            "- confidence는 HIGH, MEDIUM, LOW 중 하나여야 합니다.",
            "- confidence가 LOW이면 reviewRequired는 반드시 true여야 합니다.",
            "- 학생 답안에 오개념, 핵심 개념 누락, 모범 답안과의 의미 차이가 있으면 관련 평가 항목을 만점으로 줄 수 없습니다.",
            "- reviewReasons에 점수 조정 필요성, 오개념, 누락, 불확실성을 적었다면 관련 rubricScores에서 반드시 감점해야 합니다.",
            "- 학생 답안이 부분적으로 맞더라도 정의가 틀렸거나 과도하게 단정적이면 개념 이해 항목에서 감점하세요.",
            "- 피드백, 채점 사유, 학습 포인트, 재검토 사유는 모두 한국어로 작성하세요.",
            "",
            "문제 제목:",
            request.questionTitle,
            "",
            "문제:",
            request.questionContent,
            "",
            "모범 답안:",
            request.modelAnswer,
            "",
            "학생 답안:",
            request.studentAnswer,
            "",
            "총점:",
            request.totalScore.toString(),
            "",
            "평가 기준:",
            rubricText,
        ).joinToString("\n")
    }

    private fun extractModelText(rawResponse: String): String {
        val root = objectMapper.readTree(rawResponse)
        val generatedText = root.at("/candidates/0/content/parts/0/text").asText(null)

        return generatedText
            ?.takeIf { it.isNotBlank() }
            ?.trim()
            ?: error(
                "Gemini response has no text content. " +
                    "path=/candidates/0/content/parts/0/text, rawResponse=${rawResponse.take(300)}...",
            )
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

        private fun buildRestClient(
            restClientBuilder: RestClient.Builder,
            baseUrl: String,
            timeoutSeconds: Long,
        ): RestClient =
            restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(
                    ClientHttpRequestFactories.get(
                        ClientHttpRequestFactorySettings.DEFAULTS
                            .withConnectTimeout(Duration.ofSeconds(timeoutSeconds))
                            .withReadTimeout(Duration.ofSeconds(timeoutSeconds)),
                    ),
                ).build()

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
