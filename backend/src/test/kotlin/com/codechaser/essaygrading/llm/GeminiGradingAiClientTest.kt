package com.codechaser.essaygrading.llm

import com.codechaser.essaygrading.enums.GradingConfidence
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount.once
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class GeminiGradingAiClientTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `Gemini 응답 점수가 요청 rubric과 달라도 요청 기준으로 보정한다`() {
        val restClientBuilder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(restClientBuilder).build()
        val client =
            GeminiGradingAiClient(
                objectMapper = objectMapper,
                restClient = restClientBuilder.baseUrl("https://generativelanguage.googleapis.com/v1beta").build(),
                apiKey = "test-gemini-key",
                model = "gemini-2.5-flash",
            )

        val requestExpectation =
            server.expect(
                once(),
                requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"),
            )
        requestExpectation.andExpect(method(HttpMethod.POST))
        requestExpectation.andExpect(header("x-goog-api-key", "test-gemini-key"))
        requestExpectation.andExpect(
            content().json("""{"generationConfig":{"responseMimeType":"application/json","temperature":0.0}}"""),
        )
        requestExpectation.andExpect(
            jsonPath("$.contents[0].parts[0].text")
                .value(containsString("한국어 서술형 답안")),
        )
        requestExpectation.andExpect(
            jsonPath("$.contents[0].parts[0].text")
                .value(containsString("오개념")),
        )
        requestExpectation.andExpect(
            jsonPath("$.contents[0].parts[0].text")
                .value(containsString("관련 rubricScores에서 반드시 감점")),
        )
        requestExpectation.andExpect(
            jsonPath("$.contents[0].parts[0].text")
                .value(containsString("핵심 오개념이 있으면 관련 평가 항목 점수는 최대 배점의 70% 이하")),
        )
        requestExpectation.andExpect(
            jsonPath("$.contents[0].parts[0].text")
                .value(containsString("조건부 설명, 상쇄 관계, 원인과 결과")),
        )
        requestExpectation.andRespond(withSuccess(geminiResponse(), MediaType.APPLICATION_JSON))

        val response = client.grade(sampleRequest())

        assertEquals(82, response.totalScore)
        assertEquals(100, response.maxScore)
        assertEquals("gemini-2.5-flash", response.modelName)
        assertEquals("gemini-grading-v2", response.promptVersionName)
        assertEquals(GradingConfidence.MEDIUM, response.confidence)
        assertFalse(response.reviewRequired)
        assertEquals(2, response.rubricScores.size)
        assertEquals("개념 이해", response.rubricScores.first().rubricItemName)
        assertEquals(listOf("개념 이해", "실천 방안"), response.deductions.map { it.rubricItemName })
        assertEquals(listOf(12, 6), response.deductions.map { it.pointsLost })
        assertEquals(
            "정의 설명이 부족합니다. / 실질 배출량 0 설명이 빠졌습니다.",
            response.deductions.first { it.rubricItemName == "개념 이해" }.reason,
        )

        server.verify()
    }

    @Test
    fun `Gemini API 키가 없으면 명확한 예외를 던진다`() {
        val client =
            GeminiGradingAiClient(
                objectMapper = objectMapper,
                restClient = RestClient.builder().build(),
                apiKey = "",
                model = "gemini-2.5-flash",
            )

        val exception =
            assertThrows(IllegalStateException::class.java) {
                client.grade(sampleRequest())
            }

        assertEquals("GEMINI_API_KEY is required when LLM_PROVIDER=gemini.", exception.message)
    }

    @Test
    fun `Gemini 응답에 text가 없으면 명확한 예외를 던진다`() {
        val restClientBuilder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(restClientBuilder).build()
        val client =
            GeminiGradingAiClient(
                objectMapper = objectMapper,
                restClient = restClientBuilder.baseUrl("https://generativelanguage.googleapis.com/v1beta").build(),
                apiKey = "test-gemini-key",
                model = "gemini-2.5-flash",
            )

        val requestExpectation =
            server.expect(
                once(),
                requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"),
            )
        requestExpectation.andRespond(
            withSuccess("""{"candidates":[{"content":{"parts":[]}}]}""", MediaType.APPLICATION_JSON),
        )

        val exception =
            assertThrows(IllegalStateException::class.java) {
                client.grade(sampleRequest())
            }

        assertTrue(
            exception.message
                ?.contains("Gemini response has no text content. path=/candidates/0/content/parts/0/text") == true,
        )

        server.verify()
    }

    private fun sampleRequest(): GradingAiRequest =
        GradingAiRequest(
            questionId = 1L,
            questionTitle = "탄소 중립의 의미 설명",
            questionContent = "탄소 중립이 무엇인지 설명하고, 실천 방법을 한 가지 이상 서술하시오.",
            modelAnswer = "탄소 중립은 배출한 탄소만큼 흡수하거나 감축해 실질 배출량을 0으로 만드는 것이다.",
            studentAnswer = "탄소 중립은 탄소 배출을 줄이고 나무를 심는 것이다.",
            totalScore = 100,
            rubricItems =
                listOf(
                    GradingAiRequest.RubricItem(
                        name = "개념 이해",
                        criteria = "탄소 중립의 의미를 정확히 설명한다.",
                        maxScore = 60,
                        sortOrder = 1,
                    ),
                    GradingAiRequest.RubricItem(
                        name = "실천 방안",
                        criteria = "실천 방법을 구체적으로 제시한다.",
                        maxScore = 40,
                        sortOrder = 2,
                    ),
                ),
        )

    private fun geminiResponse(): String =
        """
        {
          "candidates": [
            {
              "content": {
                "parts": [
                  {
                    "text": "{\"totalScore\":20,\"maxScore\":20,\"rubricScores\":[{\"rubricItemName\":\" 개념 이해 \",\"score\":48,\"maxScore\":10,\"reason\":\"핵심 의미는 일부 설명했지만 상쇄 개념이 부족합니다.\",\"extraRubricNote\":\"ignored\"},{\"rubricItemName\":\" 실천 방안 \",\"score\":34,\"maxScore\":10,\"reason\":\"실천 예시는 제시했지만 구체성이 조금 부족합니다.\"}],\"deductions\":[{\"rubricItemName\":\" 개념 이해 \",\"pointsLost\":6,\"reason\":\"정의 설명이 부족합니다.\",\"extraDeductionNote\":\"ignored\"},{\"rubricItemName\":\"개념 이해\",\"pointsLost\":6,\"reason\":\"실질 배출량 0 설명이 빠졌습니다.\"},{\"rubricItemName\":\"전반적인 완성도\",\"pointsLost\":1,\"reason\":\"등록되지 않은 항목명입니다.\"}],\"studentFeedback\":\"핵심 방향은 맞지만 탄소 중립의 정의를 더 정확히 써야 합니다.\",\"learningPoints\":[\"실질 배출량 0의 의미를 복습하세요.\",\"실천 방안을 구체적 행동으로 설명하세요.\"],\"confidence\":\"MEDIUM\",\"reviewRequired\":false,\"reviewReasons\":[],\"extraModelNote\":\"ignored\"}"
                  }
                ]
              }
            }
          ]
        }
        """.trimIndent()
}
