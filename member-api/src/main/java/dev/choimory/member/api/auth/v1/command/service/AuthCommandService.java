package dev.choimory.member.api.auth.v1.command.service;

import dev.choimory.member.api.auth.v1.command.handler.AuthCommandHandler;
import dev.choimory.member.api.auth.v1.command.payload.response.LoginResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 인증 토큰 명령 API의 비즈니스 로직 흐름을 담당하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class AuthCommandService {

    private final AuthCommandHandler authCommandHandler; // 인증 토큰 명령 핸들러

    /**
     * 인증된 회원의 토큰을 발급합니다. 로그인 필터가 인증에 성공한 뒤 호출합니다.
     *
     * @param memberId 회원 ID
     * @param authorities 권한 코드 목록
     * @return 로그인 응답
     */
    public LoginResponse issueTokens(String memberId, List<String> authorities) {
        return LoginResponse.from(authCommandHandler.issueTokens(memberId, authorities));
    }
}
