package dev.choimory.member.api.member.v1.command.service;

import dev.choimory.member.api.authority.v1.command.handler.AuthorityCommandHandler;
import dev.choimory.member.api.jwt.v1.command.handler.JwtCommandHandler;
import dev.choimory.member.api.jwt.v1.command.payload.dto.JwtClaims;
import dev.choimory.member.api.jwt.v1.command.util.JwtProvider;
import dev.choimory.member.api.member.v1.command.handler.MemberCommandHandler;
import dev.choimory.member.api.member.v1.command.payload.request.MemberJoinRequest;
import dev.choimory.member.api.member.v1.command.payload.request.MemberReissueRequest;
import dev.choimory.member.api.member.v1.command.payload.response.MemberJoinResponse;
import dev.choimory.member.api.member.v1.command.payload.response.MemberTokenResponse;
import dev.choimory.member.api.security.user.AuthenticatedMember;
import dev.choimory.member.domain.authority.command.dto.AuthorityDto;
import dev.choimory.member.domain.authority.command.type.AuthorityCode;
import dev.choimory.member.domain.member.command.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 명령 API의 비즈니스 로직 흐름을 담당하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class MemberCommandService {

    private final MemberCommandHandler memberCommandHandler; // 회원 명령 핸들러
    private final AuthorityCommandHandler authorityCommandHandler; // 권한 명령 핸들러
    private final JwtCommandHandler jwtCommandHandler; // JWT 명령 핸들러

    /**
     * 회원가입을 처리합니다.
     *
     * @param request 회원가입 요청
     * @return 회원가입 응답
     */
    @Transactional
    public MemberJoinResponse join(MemberJoinRequest request) {
        // 1. 이메일, 닉네임 중복 검증
        memberCommandHandler.validateJoin(request);

        // TODO 2. 약관 검증 (필수 약관 동의 여부)

        // 3. 기본 권한 조회
        AuthorityDto authority = authorityCommandHandler.findByCode(AuthorityCode.MEMBER);

        // 4. 회원 저장
        MemberDto member = memberCommandHandler.join(request, authority);

        // TODO 5. 약관 동의 이력 저장
        // TODO 6. 이메일 인증 메일 발송 queue 전송

        // 7. 응답 객체 변환
        return MemberJoinResponse.from(member);
    }

    /**
     * Refresh Token으로 Access Token과 Refresh Token을 다시 발급합니다. (rotation)
     * 새 Access Token에는 회원의 현재 권한이 반영되며, 로그인할 수 없는 상태가 된 회원은 재발급할 수 없습니다.
     *
     * @param request 재발급 요청
     * @return 새로 발급된 토큰
     * @throws dev.choimory.member.api.common.exception.CommonException 만료는 TOKEN_EXPIRED, 그 외는 INVALID_TOKEN (401)
     */
    public MemberTokenResponse reissue(MemberReissueRequest request) {
        // 1. Refresh Token 검증과 사용 처리
        JwtClaims claims = jwtCommandHandler.consumeRefreshToken(request.refreshToken());

        // 2. 회원의 현재 상태와 권한 확인. 없거나 삭제되었거나 로그인할 수 없는 상태의 회원은 재발급할 수 없다.
        AuthenticatedMember member = memberCommandHandler
                .findById(claims.memberId())
                .map(AuthenticatedMember::from)
                .filter(AuthenticatedMember::isEnabled)
                .orElseThrow(JwtProvider::invalidToken);

        // 3. 새 토큰 발급
        return MemberTokenResponse.from(jwtCommandHandler.issueTokens(member.getId(), member.authorityCodes()));
    }
}
