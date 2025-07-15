package io.choimory.member.external.api.common.exception

import io.choimory.member.external.api.common.domain.dto.CommonValidateResult
import io.choimory.member.external.api.common.domain.response.CommonResponse
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice("io.choimory")
class CommonExceptionHandler {
    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun exception(): CommonResponse<String> {
        TODO()
    }

    @ExceptionHandler(RuntimeException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun runtimeException(): CommonResponse<String> {
        TODO()
    }

    @ExceptionHandler(CommonException::class)
    fun commonException(): CommonResponse<String> {
        TODO()
    }

    // Spring validation
    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun methodArgumentNotValidException(): CommonResponse<List<CommonValidateResult>> {
        TODO()
    }

    // Spring validation
    @ExceptionHandler(ConstraintViolationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun constraintViolationException(): CommonResponse<List<CommonValidateResult>> {
        TODO()
    }

    // Spring security
    @ExceptionHandler(AuthenticationException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun authenticateException() {
        TODO()
    }
}
