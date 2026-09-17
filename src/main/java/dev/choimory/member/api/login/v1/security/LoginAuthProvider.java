package dev.choimory.member.api.login.v1.security;

import dev.choimory.member.api.common.exception.CommonException;
import dev.choimory.member.api.member.v1.query.domain.dto.MemberDocumentDto;
import dev.choimory.member.api.member.v1.query.service.MemberQueryService;
import dev.choimory.member.api.token.v1.domain.TokenDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * 로그인 요청의 이메일과 비밀번호를 검증하는 인증 Provider입니다.
 */
@Component
@RequiredArgsConstructor
public class LoginAuthProvider implements AuthenticationProvider {

    private final MemberQueryService memberQueryService; // 회원 query 서비스

    /**
     * 로그인 인증을 처리합니다.
     *
     * @param authentication 인증 요청 객체
     * @return 인증 완료 객체
     * @throws AuthenticationException 인증 실패 시 발생
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        // Security 표준 객체에서 이메일과 비밀번호를 획득한다.
        String email = authentication != null ? authentication.getName() : null;
        String password = authentication != null && authentication.getCredentials() != null
                ? authentication.getCredentials().toString()
                : null;

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new CommonException(
                    HttpStatus.BAD_REQUEST,
                    HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.name(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase());
        }

        // 별도 비즈니스 로직으로 로그인 정보를 검증한다.
        MemberDocumentDto member = memberQueryService.login(email, password);

        // 토큰 Claim에 사용할 상세 객체로 변환한다.
        TokenDetails details = TokenDetails.of(member);

        // Success handler로 인증 결과를 전달한다.
        return new UsernamePasswordAuthenticationToken(email, details);
    }

    /**
     * Provider가 지원하는 인증 객체 타입인지 확인합니다.
     *
     * @param authentication 인증 객체 타입
     * @return 지원 여부
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
