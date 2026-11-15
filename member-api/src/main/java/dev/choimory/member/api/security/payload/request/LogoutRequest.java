package dev.choimory.member.api.security.payload.request;

import lombok.Builder;

/**
 * 로그아웃 요청 객체입니다. 로그아웃 필터가 JSON 본문을 읽어 사용합니다.
 *
 * @param refreshToken 폐기할 Refresh Token
 */
@Builder(toBuilder = true)
public record LogoutRequest(String refreshToken) {}
