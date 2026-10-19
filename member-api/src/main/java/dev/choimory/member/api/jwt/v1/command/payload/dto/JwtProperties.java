package dev.choimory.member.api.jwt.v1.command.payload.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT 설정값입니다. (application-jwt.yml)
 *
 * @param secret 서명 키 (HS256, 32바이트 이상)
 * @param issuer 발급자
 * @param accessExpiration Access Token 만료 시간
 * @param refreshExpiration Refresh Token 만료 시간
 */
@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        @NotBlank String secret,
        @NotBlank String issuer,
        @NotNull Duration accessExpiration,
        @NotNull Duration refreshExpiration) {}
