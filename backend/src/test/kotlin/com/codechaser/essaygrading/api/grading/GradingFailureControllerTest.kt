package com.codechaser.essaygrading.api.grading

import com.codechaser.essaygrading.enums.GradingStatus
import com.codechaser.essaygrading.repository.GradingRequestRepository
import com.codechaser.essaygrading.repository.GradingResultRepository
import com.codechaser.essaygrading.repository.QuestionRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:grading-failure-api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "llm.provider=openai",
    ],
)
class GradingFailureControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var gradingResultRepository: GradingResultRepository

    @Autowired
    private lateinit var gradingRequestRepository: GradingRequestRepository

    @Autowired
    private lateinit var questionRepository: QuestionRepository

    @BeforeEach
    fun setUp() {
        gradingResultRepository.deleteAll()
        gradingRequestRepository.deleteAll()
        questionRepository.deleteAll()
    }

    @Test
    fun `AI Provider 예외가 발생해도 채점 요청을 FAILED로 기록한다`() {
        val questionId = createQuestion()

        mockMvc
            .perform(
                post("/api/grading-requests")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "questionId": $questionId,
                          "studentAnswer": "탄소 중립은 실질 배출량을 줄이는 것입니다."
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isInternalServerError)

        val gradingRequests = gradingRequestRepository.findAll()

        assertEquals(1, gradingRequests.size)
        assertEquals(GradingStatus.FAILED, gradingRequests.single().status)
        assertTrue(gradingRequests.single().errorMessage?.contains("OpenAI grading provider") == true)
        assertEquals(0, gradingResultRepository.count())
    }

    private fun createQuestion(): Long {
        val result =
            mockMvc
                .perform(
                    post("/api/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sampleQuestionRequest()),
                ).andExpect(status().isCreated)
                .andReturn()

        return objectMapper
            .readTree(result.response.contentAsString)
            .get("id")
            .asLong()
    }

    private fun sampleQuestionRequest(): String =
        """
        {
          "title": "탄소 중립의 의미 설명",
          "subject": "science",
          "content": "탄소 중립이 무엇인지 설명하고, 실천 방법을 두 가지 이상 서술하시오.",
          "modelAnswer": "탄소 중립은 배출한 이산화탄소의 양만큼 흡수하거나 감축하여 실질 배출량을 0으로 만드는 것이다.",
          "totalScore": 100,
          "rubricItems": [
            {
              "name": "개념 이해",
              "criteria": "탄소 중립의 의미를 정확히 설명한다.",
              "maxScore": 40,
              "sortOrder": 1
            },
            {
              "name": "실천 방안",
              "criteria": "실천 방법을 두 가지 이상 구체적으로 제시한다.",
              "maxScore": 40,
              "sortOrder": 2
            },
            {
              "name": "표현 명확성",
              "criteria": "문장이 명확하고 논리적으로 구성되어 있다.",
              "maxScore": 20,
              "sortOrder": 3
            }
          ]
        }
        """.trimIndent()
}
