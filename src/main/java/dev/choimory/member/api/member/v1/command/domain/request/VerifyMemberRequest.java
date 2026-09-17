package dev.choimory.member.api.member.v1.command.domain.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * 회원 인증 요청 객체입니다.
 */
@Builder(toBuilder = true)
public record VerifyMemberRequest(
        @JsonProperty("email") String email, // 이메일
        @JsonProperty("uuid") String uuid, // 가입 대기 UUID
        @JsonProperty("verifyCode") int verifyCode // 인증 코드
) {
}
