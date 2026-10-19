package dev.choimory.member.api.jwt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 토큰 종류입니다. 토큰의 {@code type} 클레임에 값이 담기며, Access Token 자리에 Refresh Token을 쓰는 것을 막습니다.
 */
@Getter
@RequiredArgsConstructor
public enum TokenType {
    ACCESS("access"), // 요청 인증에 쓰는 토큰
    REFRESH("refresh"); // Access Token 재발급에 쓰는 토큰

    private final String claim; // type 클레임 값
}
