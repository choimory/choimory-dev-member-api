package dev.choimory.member.api.auth.v1.command.payload.response;

import dev.choimory.member.api.jwt.IssuedTokens;
import java.time.Duration;
import java.time.Instant;
import lombok.Builder;

/**
 * 로그인 응답 객체입니다.
 *
 * @param accessToken Access Token
 * @param refreshToken Refresh Token
 * @param tokenType 토큰 종류 (Authorization 헤더 접두어)
 * @param accessExpiresIn Access Token 만료까지 남은 시간(초)
 * @param refreshExpiresIn Refresh Token 만료까지 남은 시간(초)
 */
@Builder(toBuilder = true)
public record LoginResponse(
        String accessToken, String refreshToken, String tokenType, long accessExpiresIn, long refreshExpiresIn) {

    private static final String BEARER = "Bearer"; // 토큰 종류

    /**
     * 발급된 토큰을 로그인 응답으로 변환합니다.
     *
     * @param tokens 발급된 토큰
     * @return 로그인 응답
     */
    public static LoginResponse from(IssuedTokens tokens) {
        Instant now = Instant.now();
        return LoginResponse.builder()
                .accessToken(tokens.accessToken().token())
                .refreshToken(tokens.refreshToken().token())
                .tokenType(BEARER)
                .accessExpiresIn(
                        Duration.between(now, tokens.accessToken().expiresAt()).getSeconds())
                .refreshExpiresIn(
                        Duration.between(now, tokens.refreshToken().expiresAt()).getSeconds())
                .build();
    }
}
