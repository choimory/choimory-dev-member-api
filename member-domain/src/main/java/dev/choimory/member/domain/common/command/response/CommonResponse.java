package dev.choimory.member.domain.common.command.response;

import lombok.Builder;

/**
 * API 공통 응답 객체입니다.
 *
 * @param <T> 응답 데이터 타입
 * @param code 응답 코드
 * @param name 응답 코드명
 * @param message 응답 메시지
 * @param data 응답 데이터
 */
@Builder(toBuilder = true)
public record CommonResponse<T>(int code, String name, String message, T data) {

    /**
     * 데이터가 없는 API 공통 응답을 생성합니다.
     *
     * @param code 응답 코드
     * @param name 응답 코드명
     * @param message 응답 메시지
     */
    public CommonResponse(int code, String name, String message) {
        this(code, name, message, null);
    }
}
