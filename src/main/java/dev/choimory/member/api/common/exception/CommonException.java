package dev.choimory.member.api.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * API 공통 예외 객체입니다.
 */
@Getter
public class CommonException extends RuntimeException {

    private final HttpStatus status; // HTTP 상태
    private final Integer code; // 응답 코드
    private final String codeName; // 응답 코드명
    private final String msg; // 응답 메시지

    /**
     * HTTP 상태를 기반으로 공통 예외를 생성합니다.
     *
     * @param status HTTP 상태
     */
    public CommonException(HttpStatus status) {
        this(status, status.value(), status.name(), status.getReasonPhrase());
    }

    /**
     * 세부 응답 값을 지정해 공통 예외를 생성합니다.
     *
     * @param status HTTP 상태
     * @param code 응답 코드
     * @param codeName 응답 코드명
     * @param msg 응답 메시지
     */
    public CommonException(
            HttpStatus status,
            Integer code,
            String codeName,
            String msg
    ) {
        super(msg);
        this.status = status;
        this.code = code;
        this.codeName = codeName;
        this.msg = msg;
    }
}
