package dev.choimory.member.api.member.v1.command.domain.request;

import lombok.Builder;

/**
 * 회원 인증 요청 객체입니다.
 */
@Builder(toBuilder = true)
public record VerifyMemberRequest(
        String email, // 이메일
        String uuid, // 가입 대기 UUID
        int verifyCode // 인증 코드
) {
}
