package dev.choimory.member.api.common.exception;

import dev.choimory.member.domain.common.command.response.CommonResponse;
import dev.choimory.member.domain.common.command.response.CommonValidateResult;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
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
     * 내부 정보 노출을 막기 위해 예외 메시지는 응답에 담지 않고 로그로만 남깁니다.
     *
     * @param e 일반 예외
     * @return 공통 오류 응답
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CommonResponse<String> exception(Exception e) {
        // 예외를 로그로 남긴다.
        log.error("Unhandled exception", e);

        // 내부 서버 오류 응답으로 변환한다.
        return new CommonResponse<>(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.name(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()
                // , e.getMessage() // 내부 정보(DB 오류, 쿼리, 클래스 정보 등)가 노출되면 안 되어 응답에서 제외
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
        // 예외에 지정된 값이 없으면 HTTP 상태의 기본값을 사용한다.
        CommonResponse<String> response = new CommonResponse<>(
                e.getCode() != null ? e.getCode() : e.getStatus().value(),
                e.getCodeName() != null ? e.getCodeName() : e.getStatus().name(),
                e.getMsg() != null ? e.getMsg() : e.getStatus().getReasonPhrase());

        // 예외에 지정된 HTTP 상태로 응답한다.
        return new ResponseEntity<>(response, e.getStatus());
    }

    /**
     * 읽을 수 없는 요청 본문(JSON 형식이 아니거나 필드 타입이 맞지 않음)을 잘못된 요청 응답으로 변환합니다.
     * 처리하지 않으면 일반 예외 핸들러에 걸려 500이 됩니다.
     *
     * @param e 요청 본문을 읽지 못한 예외
     * @return 잘못된 요청 응답
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResponse<String> httpMessageNotReadableException(HttpMessageNotReadableException e) {
        // 잘못된 요청 응답으로 변환한다. 본문 내용은 응답에 담지 않는다.
        return new CommonResponse<>(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.name(),
                HttpStatus.BAD_REQUEST.getReasonPhrase());
    }

    /**
     * Request body validation 예외를 검증 실패 응답으로 변환합니다.
     *
     * @param e validation 예외
     * @return 검증 실패 목록 응답
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResponse<List<CommonValidateResult>> methodArgumentNotValidException(
            MethodArgumentNotValidException e) {
        // 필드 오류를 검증 실패 정보 목록으로 변환한다.
        List<CommonValidateResult> results = e.getFieldErrors().stream()
                .map(fieldError -> CommonValidateResult.builder()
                        .field(fieldError.getField())
                        .rejectValue(fieldError.getRejectedValue())
                        .message(fieldError.getDefaultMessage())
                        .build())
                .toList();

        // 검증 실패 목록을 담아 잘못된 요청 응답으로 변환한다.
        return new CommonResponse<>(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.name(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                results);
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
        // 제약 조건 위반을 검증 실패 정보 목록으로 변환한다.
        List<CommonValidateResult> results = e.getConstraintViolations().stream()
                .map(violation -> CommonValidateResult.builder()
                        .field(violation.getPropertyPath().toString())
                        .rejectValue(violation.getInvalidValue())
                        .message(violation.getMessage())
                        .build())
                .toList();

        // 검증 실패 목록을 담아 잘못된 요청 응답으로 변환한다.
        return new CommonResponse<>(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.name(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                results);
    }

    /**
     * 메서드 보안(@PreAuthorize 등)이 던진 접근 거부 예외를 권한 없음 응답으로 변환합니다.
     * 컨트롤러 안에서 발생한 예외는 Security 필터의 접근 거부 핸들러가 아니라 이 어드바이스로 오므로,
     * 처리하지 않으면 일반 예외 핸들러에 걸려 500이 됩니다.
     *
     * @param e 접근 거부 예외
     * @return 권한 없음 응답
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public CommonResponse<String> accessDeniedException(AccessDeniedException e) {
        // 권한 없음 응답으로 변환한다.
        return new CommonResponse<>(
                HttpStatus.FORBIDDEN.value(), HttpStatus.FORBIDDEN.name(), HttpStatus.FORBIDDEN.getReasonPhrase());
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
        // 인증 실패 응답으로 변환한다.
        return new CommonResponse<>(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.name(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase()
                // , e.getMessage() // 인증 실패 사유(계정 존재 여부 등)가 노출되면 안 되어 응답에서 제외
                );
    }
}
