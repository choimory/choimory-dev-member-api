package dev.choimory.member.api.common.exception;

import dev.choimory.member.api.common.domain.dto.CommonValidateResult;
import dev.choimory.member.api.common.domain.response.CommonResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * API 공통 예외를 HTTP 응답으로 변환하는 핸들러입니다.
 */
@Slf4j
@RestControllerAdvice("dev.choimory")
public class CommonExceptionHandler {

    /**
     * 처리되지 않은 일반 예외를 내부 서버 오류 응답으로 변환합니다.
     *
     * @param e 일반 예외
     * @return 공통 오류 응답
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CommonResponse<String> exception(Exception e) {
        log.error("Unhandled exception", e);
        return CommonResponse.error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                e.getMessage()
        );
    }

    /**
     * RuntimeException을 내부 서버 오류 응답으로 변환합니다.
     *
     * @param e 런타임 예외
     * @return 공통 오류 응답
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CommonResponse<String> runtimeException(RuntimeException e) {
        log.error("Runtime exception", e);
        return CommonResponse.error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                e.getMessage()
        );
    }

    /**
     * CommonException을 지정된 HTTP 상태 응답으로 변환합니다.
     *
     * @param e 공통 예외
     * @return 공통 오류 응답 엔티티
     */
    @ExceptionHandler(CommonException.class)
    public ResponseEntity<CommonResponse<String>> commonException(CommonException e) {
        CommonResponse<String> response = CommonResponse.<String>builder()
                .code(e.getCode() != null ? e.getCode() : e.getStatus().value())
                .name(e.getCodeName() != null ? e.getCodeName() : e.getStatus().name())
                .message(e.getMsg() != null ? e.getMsg() : e.getStatus().getReasonPhrase())
                .data(null)
                .build();
        return new ResponseEntity<>(response, e.getStatus());
    }

    /**
     * Request body validation 예외를 검증 실패 응답으로 변환합니다.
     *
     * @param e validation 예외
     * @return 검증 실패 목록 응답
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResponse<List<CommonValidateResult>> methodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<CommonValidateResult> results = e.getFieldErrors().stream()
                .map(fieldError -> CommonValidateResult.builder()
                        .field(fieldError.getField())
                        .rejectValue(fieldError.getRejectedValue())
                        .message(fieldError.getDefaultMessage())
                        .build())
                .toList();

        return CommonResponse.error(
                HttpStatus.BAD_REQUEST,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                results
        );
    }

    /**
     * Constraint validation 예외를 검증 실패 응답으로 변환합니다.
     *
     * @param e constraint validation 예외
     * @return 검증 실패 목록 응답
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResponse<List<CommonValidateResult>> constraintViolationException(ConstraintViolationException e) {
        List<CommonValidateResult> results = e.getConstraintViolations().stream()
                .map(violation -> CommonValidateResult.builder()
                        .field(violation.getPropertyPath().toString())
                        .rejectValue(violation.getInvalidValue())
                        .message(violation.getMessage())
                        .build())
                .toList();

        return CommonResponse.error(
                HttpStatus.BAD_REQUEST,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                results
        );
    }

    /**
     * Spring Security 인증 예외를 인증 실패 응답으로 변환합니다.
     *
     * @param e 인증 예외
     * @return 인증 실패 응답
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public CommonResponse<String> authenticateException(AuthenticationException e) {
        return CommonResponse.error(
                HttpStatus.UNAUTHORIZED,
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                e.getMessage()
        );
    }
}
