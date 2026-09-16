package dev.choimory.member.api.common.domain.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * API 공통 응답 객체입니다.
 *
 * @param <T> 응답 데이터 타입
 */
@Getter
@Builder(toBuilder = true)
public final class CommonResponse<T> {

    private final int code; // 응답 코드
    private final String name; // 응답 코드명
    private final String message; // 응답 메시지
    private final T data; // 응답 데이터

    /**
     * API 공통 응답을 생성합니다.
     *
     * @param code 응답 코드
     * @param name 응답 코드명
     * @param message 응답 메시지
     * @param data 응답 데이터
     */
    @JsonCreator
    public CommonResponse(
            @JsonProperty("code") int code,
            @JsonProperty("name") String name,
            @JsonProperty("message") String message,
            @JsonProperty("data") T data
    ) {
        this.code = code;
        this.name = name;
        this.message = message;
        this.data = data;
    }

    /**
     * 데이터가 없는 API 공통 응답을 생성합니다.
     *
     * @param code 응답 코드
     * @param name 응답 코드명
     * @param message 응답 메시지
     */
    public CommonResponse(
            int code,
            String name,
            String message
    ) {
        this(code, name, message, null);
    }

    /**
     * HTTP 상태와 메시지, 응답 데이터로 API 공통 응답을 생성합니다.
     *
     * @param status HTTP 상태
     * @param message 응답 메시지
     * @param data 응답 데이터
     * @return API 공통 응답
     * @param <T> 응답 데이터 타입
     */
    public static <T> CommonResponse<T> of(
            HttpStatus status,
            String message,
            T data
    ) {
        return CommonResponse.<T>builder()
                .code(status.value())
                .name(status.name())
                .message(message)
                .data(data)
                .build();
    }

    /**
     * 성공 API 공통 응답을 생성합니다.
     *
     * @param data 응답 데이터
     * @return 성공 API 공통 응답
     * @param <T> 응답 데이터 타입
     */
    public static <T> CommonResponse<T> ok(T data) {
        return of(HttpStatus.OK, HttpStatus.OK.getReasonPhrase(), data);
    }

    /**
     * 생성 완료 API 공통 응답을 생성합니다.
     *
     * @param data 응답 데이터
     * @return 생성 완료 API 공통 응답
     * @param <T> 응답 데이터 타입
     */
    public static <T> CommonResponse<T> created(T data) {
        return of(HttpStatus.CREATED, HttpStatus.CREATED.getReasonPhrase(), data);
    }

    /**
     * 데이터가 없는 오류 API 공통 응답을 생성합니다.
     *
     * @param status HTTP 상태
     * @param message 응답 메시지
     * @return 오류 API 공통 응답
     */
    public static CommonResponse<String> error(
            HttpStatus status,
            String message
    ) {
        return CommonResponse.<String>builder()
                .code(status.value())
                .name(status.name())
                .message(message)
                .data(null)
                .build();
    }

    /**
     * 오류 데이터가 있는 API 공통 응답을 생성합니다.
     *
     * @param status HTTP 상태
     * @param message 응답 메시지
     * @param data 오류 데이터
     * @return 오류 API 공통 응답
     * @param <T> 오류 데이터 타입
     */
    public static <T> CommonResponse<T> error(
            HttpStatus status,
            String message,
            T data
    ) {
        return of(status, message, data);
    }
}
