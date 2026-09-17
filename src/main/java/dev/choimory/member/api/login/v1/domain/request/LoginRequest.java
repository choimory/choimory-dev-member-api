package dev.choimory.member.api.login.v1.domain.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * 로그인 요청 객체입니다.
 */
@Builder(toBuilder = true)
public record LoginRequest(
        @JsonProperty("email") String email, // 이메일
        @JsonProperty("password") String password // 비밀번호
) {
}
