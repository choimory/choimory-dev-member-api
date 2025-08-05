package dev.choimory.member.api.common.exception

import dev.choimory.member.api.common.domain.dto.CommonValidateResult
import dev.choimory.member.api.common.domain.response.CommonResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice("dev.choimory")
class CommonExceptionHandler {
    private val log = KotlinLogging.logger { }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun exception(e: Exception): CommonResponse<String> {
        log.error { "${e.message}" }
        return CommonResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase,
            e.message,
        )
    }

    @ExceptionHandler(RuntimeException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun runtimeException(e: RuntimeException): CommonResponse<String> {
        log.error { "${e.message}" }
        return CommonResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase,
            e.message,
        )
    }

    @ExceptionHandler(CommonException::class)
    fun commonException(e: CommonException): ResponseEntity<CommonResponse<String>> {
        return ResponseEntity(
            CommonResponse(
                e.code ?: e.status.value(),
                e.msg ?: e.status.reasonPhrase,
            ),
            e.status,
        )
    }

    // Spring validation
    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun methodArgumentNotValidException(e: MethodArgumentNotValidException): CommonResponse<List<CommonValidateResult>> {
        return CommonResponse(
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.reasonPhrase,
            e.fieldErrors.stream()
                .map { f ->
                    CommonValidateResult(
                        f.field,
                        f.rejectedValue,
                        f.defaultMessage,
                    )
                }.toList(),
        )
    }

    // Spring validation
    @ExceptionHandler(ConstraintViolationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun constraintViolationException(e: ConstraintViolationException): CommonResponse<List<CommonValidateResult>> {
        return CommonResponse(
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.reasonPhrase,
            e.constraintViolations.stream()
                .map { v ->
                    CommonValidateResult(
                        v.propertyPath.toString(),
                        v.invalidValue,
                        v.message,
                    )
                }.toList(),
        )
    }

    // Spring security
    @ExceptionHandler(AuthenticationException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun authenticateException(e: Exception): CommonResponse<String> {
        return CommonResponse(
            HttpStatus.UNAUTHORIZED.value(),
            HttpStatus.UNAUTHORIZED.reasonPhrase,
            e.message,
        )
    }
}
