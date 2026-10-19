package dev.choimory.member.api.auth.v1.command.handler;

import dev.choimory.member.api.auth.v1.command.repository.AuthTokenRepository;
import dev.choimory.member.api.jwt.IssuedToken;
import dev.choimory.member.api.jwt.IssuedTokens;
import dev.choimory.member.api.jwt.JwtProvider;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 인증 토큰 명령의 세부 로직을 담당하는 핸들러입니다.
 */
@Component
@RequiredArgsConstructor
public class AuthCommandHandler {

    private final JwtProvider jwtProvider; // JWT 생성
    private final AuthTokenRepository authTokenRepository; // 토큰 데이터 접근 객체

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
        authTokenRepository.saveRefreshToken(
                memberId, refreshToken.jti(), Duration.between(Instant.now(), refreshToken.expiresAt()));

        return IssuedTokens.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
