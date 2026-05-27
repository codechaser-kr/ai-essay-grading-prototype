package com.codechaser.essaygrading.common.error

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        exception: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        val developerMessage =
            exception.bindingResult
                .fieldErrors
                .joinToString("; ") { it.toDeveloperMessage() }

        return errorResponse(
            status = HttpStatus.BAD_REQUEST,
            message = "요청 값을 확인해주세요.",
            developerMessage = developerMessage,
            path = request.requestURI,
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        exception: IllegalArgumentException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> =
        errorResponse(
            status = HttpStatus.BAD_REQUEST,
            message = "요청 값을 확인해주세요.",
            developerMessage = exception.message,
            path = request.requestURI,
        )

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(
        exception: NotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> =
        errorResponse(
            status = HttpStatus.NOT_FOUND,
            message = exception.message,
            developerMessage = exception.developerMessage,
            path = request.requestURI,
        )

    @ExceptionHandler(Exception::class)
    fun handleException(
        exception: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> =
        errorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            message = "서버 내부 오류가 발생했습니다.",
            developerMessage = exception.message,
            path = request.requestURI,
        )

    private fun errorResponse(
        status: HttpStatus,
        message: String,
        developerMessage: String?,
        path: String,
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(status)
            .body(
                ErrorResponse(
                    message = message,
                    developerMessage = developerMessage,
                    status = status.value(),
                    path = path,
                ),
            )
}

private fun FieldError.toDeveloperMessage(): String = "$field ${defaultMessage ?: "is invalid"}"
