package com.codechaser.essaygrading.api.question

import com.codechaser.essaygrading.repository.QuestionRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:question-api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ],
)
class QuestionControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var questionRepository: QuestionRepository

    @BeforeEach
    fun setUp() {
        questionRepository.deleteAll()
    }

    @Test
    fun `문제와 rubric item을 함께 생성하고 조회한다`() {
        val createResult =
            mockMvc
                .perform(
                    post("/api/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sampleQuestionRequest()),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.title").value("탄소 중립의 의미 설명"))
                .andExpect(jsonPath("$.rubricItems.length()").value(3))
                .andExpect(jsonPath("$.rubricItems[0].name").value("개념 이해"))
                .andReturn()

        val questionId =
            objectMapper
                .readTree(createResult.response.contentAsString)
                .get("id")
                .asLong()

        mockMvc
            .perform(get("/api/questions/$questionId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(questionId))
            .andExpect(jsonPath("$.subject").value("science"))
            .andExpect(jsonPath("$.totalScore").value(100))
            .andExpect(jsonPath("$.rubricItems[2].sortOrder").value(3))

        mockMvc
            .perform(get("/api/questions"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(questionId))
            .andExpect(jsonPath("$[0].rubricItemCount").value(3))
    }

    @Test
    fun `필수 값이 비어 있으면 400을 반환한다`() {
        val request =
            sampleQuestionRequest().replace(
                "\"title\": \"탄소 중립의 의미 설명\"",
                "\"title\": \"\"",
            )

        mockMvc
            .perform(
                post("/api/questions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("요청 값을 확인해주세요."))
            .andExpect(jsonPath("$.developerMessage", containsString("title")))
    }

    @Test
    fun `rubric 점수 합계가 총점과 다르면 400을 반환한다`() {
        val request =
            sampleQuestionRequest().replace(
                "\"totalScore\": 100",
                "\"totalScore\": 90",
            )

        mockMvc
            .perform(
                post("/api/questions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.developerMessage", containsString("rubric maxScore 합계")))
    }

    @Test
    fun `rubric 이름이 중복되면 400을 반환한다`() {
        val request =
            sampleQuestionRequest().replace(
                "\"name\": \"실천 방안\"",
                "\"name\": \" 개념 이해 \"",
            )

        mockMvc
            .perform(
                post("/api/questions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.developerMessage", containsString("평가 기준 항목 이름은 중복될 수 없습니다.")))
    }

    @Test
    fun `존재하지 않는 문제를 조회하면 404를 반환한다`() {
        mockMvc
            .perform(get("/api/questions/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("문제를 찾을 수 없습니다."))
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
