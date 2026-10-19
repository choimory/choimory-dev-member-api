package dev.choimory.member.api.jwt.v1.command.payload.dto;

import lombok.Builder;

/**
 * 로그인 때 함께 발급되는 Access Token과 Refresh Token입니다.
 *
 * @param accessToken 발급된 Access Token
 * @param refreshToken 발급된 Refresh Token
 */
@Builder(toBuilder = true)
public record IssuedTokens(IssuedToken accessToken, IssuedToken refreshToken) {}
