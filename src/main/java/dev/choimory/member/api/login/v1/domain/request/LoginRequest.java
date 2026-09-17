package dev.choimory.member.api.login.v1.domain.request;

import lombok.Builder;

/**
 * 로그인 요청 객체입니다.
 */
@Builder(toBuilder = true)
public record LoginRequest(
        String email, // 이메일
        String password // 비밀번호
        ) {}
