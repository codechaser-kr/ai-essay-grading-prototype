package com.codechaser.essaygrading.api.question

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/questions")
class QuestionController(
    private val questionService: QuestionService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createQuestion(
        @Valid @RequestBody request: Request.CreateQuestionParams,
    ): Response.QuestionData = questionService.createQuestion(request)

    @GetMapping
    fun getQuestions(): List<Response.QuestionSummaryData> = questionService.getQuestions()

    @GetMapping("/{questionId}")
    fun getQuestion(
        @PathVariable questionId: Long,
    ): Response.QuestionData = questionService.getQuestion(questionId)
}
