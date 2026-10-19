package dev.choimory.member.api.jwt;

import dev.choimory.member.api.common.exception.CommonException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * JWT를 생성하고 검증하는 컴포넌트입니다. HS256으로 서명합니다.
 * 검증 실패는 비즈니스 예외로 보고 {@link CommonException}(401)을 던집니다.
 * 만료는 TOKEN_EXPIRED, 그 외 잘못된 토큰은 INVALID_TOKEN으로 구분해 클라이언트가 재발급할지 판단하게 합니다.
 */
@Component
public class JwtProvider {

    private static final String TYPE_CLAIM = "type"; // 토큰 종류 클레임명
    private static final String AUTHORITIES_CLAIM = "authorities"; // 권한 코드 목록 클레임명

    private final JwtProperties properties; // JWT 설정
    private final SecretKey key; // 서명 키

    /**
     * 설정의 서명 키로 JWT 컴포넌트를 생성합니다.
     *
     * @param properties JWT 설정
     * @throws io.jsonwebtoken.security.WeakKeyException 서명 키가 HS256에 필요한 32바이트보다 짧은 경우
     */
    public JwtProvider(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Access Token을 발급합니다.
     *
     * @param memberId 회원 ID
     * @param authorities 권한 코드 목록 (예: MEMBER)
     * @return 발급된 Access Token
     */
    public IssuedToken createAccessToken(String memberId, List<String> authorities) {
        return create(memberId, TokenType.ACCESS, properties.accessExpiration(), authorities);
    }

    /**
     * Refresh Token을 발급합니다.
     *
     * @param memberId 회원 ID
     * @return 발급된 Refresh Token
     */
    public IssuedToken createRefreshToken(String memberId) {
        return create(memberId, TokenType.REFRESH, properties.refreshExpiration(), List.of());
    }

    /**
     * 토큰의 서명, 발급자, 만료, 종류를 검증하고 클레임을 꺼냅니다.
     *
     * @param token 토큰 문자열
     * @param expectedType 기대하는 토큰 종류
     * @return 검증된 클레임
     * @throws CommonException 만료는 TOKEN_EXPIRED(401), 그 외 잘못된 토큰은 INVALID_TOKEN(401)
     */
    public JwtClaims parse(String token, TokenType expectedType) {
        Claims claims;
        try {
            // 서명과 발급자, 만료를 검증하고 클레임을 꺼낸다.
            claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(properties.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new CommonException(HttpStatus.UNAUTHORIZED, 401, "TOKEN_EXPIRED", "만료된 토큰입니다.");
        } catch (JwtException | IllegalArgumentException e) {
            throw invalidToken();
        }

        // 기대한 종류의 토큰이어야 한다. (Access Token 자리에 Refresh Token을 쓰는 것을 막는다)
        if (!expectedType.getClaim().equals(claims.get(TYPE_CLAIM, String.class))
                || claims.getSubject() == null
                || claims.getId() == null) {
            throw invalidToken();
        }

        return JwtClaims.builder()
                .memberId(claims.getSubject())
                .jti(claims.getId())
                .type(expectedType)
                .authorities(authorities(claims))
                .expiresAt(claims.getExpiration().toInstant())
                .build();
    }

    /**
     * 토큰을 만들어 서명합니다.
     *
     * @param memberId 회원 ID
     * @param type 토큰 종류
     * @param expiration 만료까지의 시간
     * @param authorities 권한 코드 목록
     * @return 발급된 토큰
     */
    private IssuedToken create(String memberId, TokenType type, Duration expiration, List<String> authorities) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expiration);
        String jti = UUID.randomUUID().toString();

        // 클레임을 채워 서명한다. 권한은 Access Token에만 담는다.
        var builder = Jwts.builder()
                .subject(memberId)
                .id(jti)
                .issuer(properties.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim(TYPE_CLAIM, type.getClaim());
        if (type == TokenType.ACCESS) {
            builder.claim(AUTHORITIES_CLAIM, authorities);
        }

        return IssuedToken.builder()
                .token(builder.signWith(key, Jwts.SIG.HS256).compact())
                .jti(jti)
                .expiresAt(expiresAt)
                .build();
    }

    /**
     * 클레임에서 권한 코드 목록을 꺼냅니다.
     *
     * @param claims 토큰 클레임
     * @return 권한 코드 목록, 없으면 빈 목록
     */
    private List<String> authorities(Claims claims) {
        Object value = claims.get(AUTHORITIES_CLAIM);
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    /**
     * 잘못된 토큰 예외를 생성합니다. 서명 오류, 종류 불일치, 저장되지 않은 Refresh Token 등에 공통으로 사용합니다.
     *
     * @return 401 공통 예외
     */
    public static CommonException invalidToken() {
        return new CommonException(HttpStatus.UNAUTHORIZED, 401, "INVALID_TOKEN", "유효하지 않은 토큰입니다.");
    }
}
