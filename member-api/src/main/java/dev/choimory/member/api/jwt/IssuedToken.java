package dev.choimory.member.api.jwt;

import java.time.Instant;
import lombok.Builder;

/**
 * 발급된 토큰 정보입니다. Refresh Token을 Redis에 저장할 때 jti와 만료 일시가 필요해서 토큰 문자열과 함께 담습니다.
 *
 * @param token 서명된 토큰 문자열
 * @param jti 토큰 고유 ID
 * @param expiresAt 만료 일시
 */
@Builder(toBuilder = true)
public record IssuedToken(String token, String jti, Instant expiresAt) {}
