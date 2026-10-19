package dev.choimory.member.api.security.user;

import dev.choimory.member.api.member.v1.command.handler.MemberCommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 인증에 쓰는 회원 조회 서비스입니다. 이메일을 사용자 이름으로 사용합니다.
 * Security가 호출하는 서비스 역할이므로 컨트롤러를 거치지 않고 핸들러를 직접 호출합니다.
 */
@Service
@RequiredArgsConstructor
public class MemberUserDetailsService implements UserDetailsService {

    private final MemberCommandHandler memberCommandHandler; // 회원 명령 핸들러

    /**
     * 이메일로 인증 대상 회원을 조회합니다.
     *
     * @param email 이메일
     * @return 인증 대상 회원
     * @throws UsernameNotFoundException 회원이 없거나 삭제된 경우
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return memberCommandHandler
                .findByEmail(email)
                .map(AuthenticatedMember::from)
                .orElseThrow(() -> new UsernameNotFoundException("회원을 찾을 수 없습니다."));
    }
}
