package dev.choimory.member.api.member.v1.command.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * 토큰 재발급 요청 객체입니다.
 *
 * @param refreshToken 로그인 또는 이전 재발급으로 받은 Refresh Token
 */
@Builder(toBuilder = true)
public record MemberReissueRequest(
        @NotBlank(message = "Refresh Token을 입력해야 합니다.") String refreshToken) {}
