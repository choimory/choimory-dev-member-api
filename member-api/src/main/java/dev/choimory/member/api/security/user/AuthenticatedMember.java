package dev.choimory.member.api.security.user;

import dev.choimory.member.api.jwt.v1.command.payload.dto.JwtClaims;
import dev.choimory.member.domain.authority.command.dto.MemberAuthorityDto;
import dev.choimory.member.domain.member.command.dto.MemberDto;
import dev.choimory.member.domain.member.command.type.MemberStatus;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 인증된 회원입니다.
 * 로그인 때는 회원 DTO로 만들어 비밀번호 검증에 쓰고, 요청 인증 때는 Access Token의 클레임만으로 만들어 DB를 조회하지 않습니다.
 * 권한 코드는 {@code ROLE_} 접두어를 붙여 {@code hasRole}로 검사할 수 있게 합니다.
 */
@Getter
public class AuthenticatedMember implements UserDetails {

    private static final String ROLE_PREFIX = "ROLE_"; // 권한 접두어

    private final String id; // 회원 ID
    private final String email; // 이메일 (토큰 기반 인증에서는 null)
    private final String password; // 암호화된 비밀번호 (토큰 기반 인증에서는 null)
    private final List<GrantedAuthority> authorities; // 권한
    private final boolean enabled; // 로그인 가능 여부 (ACTIVATE 상태만 true)

    private AuthenticatedMember(
            String id, String email, String password, List<GrantedAuthority> authorities, boolean enabled) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
        this.enabled = enabled;
    }

    /**
     * 토큰 클레임으로 인증된 회원을 생성합니다.
     *
     * @param claims 검증된 Access Token 클레임
     * @return 인증된 회원
     */
    public static AuthenticatedMember from(JwtClaims claims) {
        return new AuthenticatedMember(claims.memberId(), null, null, toAuthorities(claims.authorities()), true);
    }

    /**
     * 로그인 인증을 위해 회원 DTO로 인증 대상 회원을 생성합니다. 가입 직후 상태인 ACTIVATE 회원만 활성입니다.
     *
     * @param member 권한을 포함한 회원 DTO
     * @return 인증 대상 회원
     */
    public static AuthenticatedMember from(MemberDto member) {
        List<String> codes = member.authorities().stream()
                .map(MemberAuthorityDto::authority)
                .map(authority -> authority.code().name())
                .toList();
        return new AuthenticatedMember(
                member.id(),
                member.email(),
                member.password(),
                toAuthorities(codes),
                member.status() == MemberStatus.ACTIVATE);
    }

    /**
     * 접두어를 뗀 권한 코드 목록을 반환합니다. 토큰의 권한 클레임에 담을 때 사용합니다.
     *
     * @return 권한 코드 목록 (예: MEMBER)
     */
    public List<String> authorityCodes() {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.substring(ROLE_PREFIX.length()))
                .toList();
    }

    /**
     * 사용자 식별자로 회원 ID를 반환합니다.
     *
     * @return 회원 ID
     */
    @Override
    public String getUsername() {
        return id;
    }

    /**
     * 권한을 반환합니다.
     *
     * @return 권한 목록
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /**
     * 권한 코드를 {@code ROLE_} 접두어를 붙인 권한 목록으로 변환합니다.
     *
     * @param codes 권한 코드 목록
     * @return 권한 목록
     */
    private static List<GrantedAuthority> toAuthorities(List<String> codes) {
        return codes.stream()
                .<GrantedAuthority>map(code -> new SimpleGrantedAuthority(ROLE_PREFIX + code))
                .toList();
    }
}
