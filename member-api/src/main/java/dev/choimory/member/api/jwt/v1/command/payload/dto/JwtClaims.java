package dev.choimory.member.api.jwt.v1.command.payload.dto;

import dev.choimory.member.api.jwt.v1.command.payload.type.TokenType;
import java.time.Instant;
import java.util.List;
import lombok.Builder;

/**
 * 검증을 마친 토큰에서 꺼낸 클레임입니다.
 *
 * @param memberId 회원 ID (sub)
 * @param jti 토큰 고유 ID
 * @param type 토큰 종류
 * @param authorities 권한 코드 목록 (Access Token에만 있고 Refresh Token은 빈 목록)
 * @param expiresAt 만료 일시
 */
@Builder(toBuilder = true)
public record JwtClaims(String memberId, String jti, TokenType type, List<String> authorities, Instant expiresAt) {}
