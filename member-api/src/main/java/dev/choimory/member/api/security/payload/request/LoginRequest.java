package dev.choimory.member.api.security.payload.request;

import lombok.Builder;

/**
 * 로그인 요청 객체입니다. 로그인 필터가 JSON 본문을 읽어 사용합니다.
 * 비밀번호 규칙이 노출되지 않도록 형식 검증은 하지 않고 비어 있는지만 확인합니다.
 *
 * @param email 이메일
 * @param password 비밀번호 (평문)
 */
@Builder(toBuilder = true)
public record LoginRequest(String email, String password) {}
