package dev.choimory.member.api.jwt.v1.command.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.choimory.member.api.common.exception.CommonException;
import dev.choimory.member.api.config.properties.JwtProperties;
import dev.choimory.member.api.jwt.v1.command.payload.dto.IssuedToken;
import dev.choimory.member.api.jwt.v1.command.payload.dto.JwtClaims;
import dev.choimory.member.api.jwt.v1.command.payload.type.TokenType;
import io.jsonwebtoken.security.WeakKeyException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * JwtProvider 단위 테스트입니다. Spring과 Docker 없이 토큰 생성과 검증을 확인합니다.
 */
class JwtProviderTest {

    private static final String SECRET = "unit-test-jwt-secret-key-at-least-32-bytes!!"; // 테스트용 서명 키
    private static final String ISSUER = "test-issuer"; // 테스트용 발급자

    private final JwtProvider provider = provider(SECRET, ISSUER, Duration.ofMinutes(30), Duration.ofDays(14));

    /**
     * 테스트용 JwtProvider를 생성합니다.
     *
     * @param secret 서명 키
     * @param issuer 발급자
     * @param access Access Token 만료 시간
     * @param refresh Refresh Token 만료 시간
     * @return JwtProvider
     */
    private static JwtProvider provider(String secret, String issuer, Duration access, Duration refresh) {
        return new JwtProvider(new JwtProperties(secret, issuer, access, refresh));
    }

    /**
     * Access Token을 발급하면 검증 후 회원 ID, 권한, 종류, 만료 일시를 꺼낼 수 있어야 합니다.
     */
    @Test
    void accessTokenRoundTrip() {
        // Access Token을 발급한다.
        IssuedToken issued = provider.createAccessToken("member-1", List.of("MEMBER", "ADMIN"));

        // 검증하면 발급할 때의 값이 그대로 나와야 한다.
        JwtClaims claims = provider.parse(issued.token(), TokenType.ACCESS);
        assertThat(claims.memberId()).isEqualTo("member-1");
        assertThat(claims.type()).isEqualTo(TokenType.ACCESS);
        assertThat(claims.authorities()).containsExactly("MEMBER", "ADMIN");
        assertThat(claims.jti()).isEqualTo(issued.jti());

        // 만료 일시는 발급 시점부터 30분 뒤여야 한다. (토큰은 초 단위로 저장되므로 1초 오차를 허용한다)
        assertThat(claims.expiresAt())
                .isBetween(Instant.now().plusSeconds(30 * 60 - 5), Instant.now().plusSeconds(30 * 60 + 5));
        assertThat(issued.expiresAt())
                .isCloseTo(
                        claims.expiresAt(),
                        org.assertj.core.api.Assertions.within(1, java.time.temporal.ChronoUnit.SECONDS));
    }

    /**
     * Refresh Token은 권한 없이 14일 뒤 만료로 발급됩니다.
     */
    @Test
    void refreshTokenHasNoAuthoritiesAndLongExpiration() {
        // Refresh Token을 발급해 검증한다.
        IssuedToken issued = provider.createRefreshToken("member-1");
        JwtClaims claims = provider.parse(issued.token(), TokenType.REFRESH);

        // 권한은 없고 만료는 14일 뒤여야 한다.
        assertThat(claims.type()).isEqualTo(TokenType.REFRESH);
        assertThat(claims.authorities()).isEmpty();
        assertThat(claims.expiresAt())
                .isAfter(Instant.now().plus(Duration.ofDays(14)).minusSeconds(5));
    }

    /**
     * 토큰마다 jti가 달라야 합니다.
     */
    @Test
    void jtiIsUniquePerToken() {
        // 같은 회원의 토큰을 두 번 발급한다.
        IssuedToken first = provider.createRefreshToken("member-1");
        IssuedToken second = provider.createRefreshToken("member-1");

        // jti와 토큰 문자열이 서로 달라야 한다.
        assertThat(first.jti()).isNotEqualTo(second.jti());
        assertThat(first.token()).isNotEqualTo(second.token());
    }

    /**
     * 만료된 토큰은 TOKEN_EXPIRED(401)로 구분됩니다.
     */
    @Test
    void expiredTokenIsTokenExpired() {
        // 이미 만료된 토큰을 발급한다.
        JwtProvider expiring = provider(SECRET, ISSUER, Duration.ofSeconds(-10), Duration.ofDays(14));
        String token = expiring.createAccessToken("member-1", List.of("MEMBER")).token();

        // 검증하면 TOKEN_EXPIRED여야 한다.
        assertThatThrownBy(() -> provider.parse(token, TokenType.ACCESS))
                .isInstanceOfSatisfying(CommonException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(e.getCodeName()).isEqualTo("TOKEN_EXPIRED");
                });
    }

    /**
     * 서명이 다르거나 변조되거나 형식이 잘못된 토큰은 INVALID_TOKEN(401)입니다.
     */
    @Test
    void tamperedOrMalformedTokensAreInvalid() {
        String token = provider.createAccessToken("member-1", List.of("MEMBER")).token();

        // 다른 키로 서명된 토큰, 서명을 바꾼 토큰, 아무 문자열, 빈 문자열은 모두 INVALID_TOKEN이어야 한다.
        String otherKey = provider(
                        "another-secret-key-at-least-32-bytes-long!!",
                        ISSUER,
                        Duration.ofMinutes(30),
                        Duration.ofDays(14))
                .createAccessToken("member-1", List.of("MEMBER"))
                .token();
        String tampered = token.substring(0, token.length() - 3) + (token.endsWith("abc") ? "xyz" : "abc");
        for (String invalid : List.of(otherKey, tampered, "not-a-jwt", "")) {
            assertThatThrownBy(() -> provider.parse(invalid, TokenType.ACCESS))
                    .isInstanceOfSatisfying(CommonException.class, e -> {
                        assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                        assertThat(e.getCodeName()).isEqualTo("INVALID_TOKEN");
                    });
        }
    }

    /**
     * 다른 발급자의 토큰은 INVALID_TOKEN입니다.
     */
    @Test
    void otherIssuerIsInvalid() {
        // 같은 키지만 다른 발급자로 발급한 토큰을 만든다.
        String token = provider(SECRET, "other-issuer", Duration.ofMinutes(30), Duration.ofDays(14))
                .createAccessToken("member-1", List.of("MEMBER"))
                .token();

        // 발급자가 달라 검증에 실패해야 한다.
        assertThatThrownBy(() -> provider.parse(token, TokenType.ACCESS))
                .isInstanceOfSatisfying(
                        CommonException.class, e -> assertThat(e.getCodeName()).isEqualTo("INVALID_TOKEN"));
    }

    /**
     * 토큰 종류가 기대와 다르면 INVALID_TOKEN입니다. (Access Token 자리에 Refresh Token을 쓰는 경우 등)
     */
    @Test
    void wrongTokenTypeIsInvalid() {
        IssuedToken access = provider.createAccessToken("member-1", List.of("MEMBER"));
        IssuedToken refresh = provider.createRefreshToken("member-1");

        // Refresh Token을 Access로, Access Token을 Refresh로 검증하면 실패해야 한다.
        assertThatThrownBy(() -> provider.parse(refresh.token(), TokenType.ACCESS))
                .isInstanceOfSatisfying(
                        CommonException.class, e -> assertThat(e.getCodeName()).isEqualTo("INVALID_TOKEN"));
        assertThatThrownBy(() -> provider.parse(access.token(), TokenType.REFRESH))
                .isInstanceOfSatisfying(
                        CommonException.class, e -> assertThat(e.getCodeName()).isEqualTo("INVALID_TOKEN"));
    }

    /**
     * 서명 키가 32바이트보다 짧으면 시작할 때 실패해야 합니다.
     */
    @Test
    void weakSecretFailsFast() {
        assertThatThrownBy(() -> provider("too-short", ISSUER, Duration.ofMinutes(30), Duration.ofDays(14)))
                .isInstanceOf(WeakKeyException.class);
    }
}
