package dev.choimory.member.api.common.domain.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

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
}
