package com.codechaser.essaygrading.api.grading

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class GradingController(
    private val gradingService: GradingService,
) {
    @PostMapping("/api/grading-requests")
    @ResponseStatus(HttpStatus.CREATED)
    fun createGradingRequest(
        @Valid @RequestBody request: Request.CreateGradingRequestParams,
    ): Response.CreateGradingRequestData = gradingService.createGradingRequest(request)

    @GetMapping("/api/grading-results/{gradingResultId}")
    fun getGradingResult(
        @PathVariable gradingResultId: Long,
    ): Response.GradingResultData = gradingService.getGradingResult(gradingResultId)

    @GetMapping("/api/grading-results")
    fun getGradingResults(
        @RequestParam questionId: Long,
    ): List<Response.GradingResultData> = gradingService.getGradingResults(questionId)
}
