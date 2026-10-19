package dev.choimory.member.api.jwt.v1.command.handler;

import dev.choimory.member.api.common.exception.CommonException;
import dev.choimory.member.api.jwt.IssuedToken;
import dev.choimory.member.api.jwt.IssuedTokens;
import dev.choimory.member.api.jwt.JwtClaims;
import dev.choimory.member.api.jwt.JwtProvider;
import dev.choimory.member.api.jwt.TokenType;
import dev.choimory.member.api.jwt.v1.command.repository.JwtRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * JWT 명령의 세부 로직을 담당하는 핸들러입니다.
 */
@Component
@RequiredArgsConstructor
public class JwtCommandHandler {

    private final JwtProvider jwtProvider; // JWT 생성과 검증
    private final JwtRepository jwtRepository; // JWT 데이터 접근 객체

    /**
     * Access Token과 Refresh Token을 발급하고 Refresh Token을 Redis에 저장합니다.
     *
     * @param memberId 회원 ID
     * @param authorities 권한 코드 목록 (예: MEMBER)
     * @return 발급된 토큰
     */
    public IssuedTokens issueTokens(String memberId, List<String> authorities) {
        // 토큰을 발급한다.
        IssuedToken accessToken = jwtProvider.createAccessToken(memberId, authorities);
        IssuedToken refreshToken = jwtProvider.createRefreshToken(memberId);

        // Refresh Token은 만료 시간까지만 Redis에 저장한다.
        jwtRepository.saveRefreshToken(
                memberId, refreshToken.jti(), Duration.between(Instant.now(), refreshToken.expiresAt()));

        return IssuedTokens.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * Refresh Token을 검증하고 사용 처리(Redis에서 삭제)합니다. 한 번 쓴 Refresh Token은 다시 쓸 수 없습니다.
     * Redis 삭제에 성공한 요청만 통과하므로 같은 토큰으로 동시에 재발급을 요청해도 하나만 성공합니다.
     *
     * @param refreshToken Refresh Token 문자열
     * @return 검증된 클레임
     * @throws CommonException 만료는 TOKEN_EXPIRED, 서명이 잘못되었거나 저장되어 있지 않은(이미 사용했거나 폐기된) 토큰은 INVALID_TOKEN (401)
     */
    public JwtClaims consumeRefreshToken(String refreshToken) {
        // 서명, 발급자, 만료, 토큰 종류를 검증한다.
        JwtClaims claims = jwtProvider.parse(refreshToken, TokenType.REFRESH);

        // 저장된 토큰을 삭제하지 못하면 이미 사용했거나 폐기된 토큰이다.
        if (!jwtRepository.deleteRefreshToken(claims.memberId(), claims.jti())) {
            throw JwtProvider.invalidToken();
        }
        return claims;
    }
}
