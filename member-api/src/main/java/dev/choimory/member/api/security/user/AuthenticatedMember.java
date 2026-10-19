package dev.choimory.member.api.security.user;

import dev.choimory.member.api.jwt.JwtClaims;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 인증된 회원입니다. Access Token의 클레임만으로 만들어지며 요청마다 DB를 조회하지 않습니다.
 * 권한 코드는 {@code ROLE_} 접두어를 붙여 {@code hasRole}로 검사할 수 있게 합니다.
 */
@Getter
public class AuthenticatedMember implements UserDetails {

    private static final String ROLE_PREFIX = "ROLE_"; // 권한 접두어

    private final String id; // 회원 ID
    private final List<GrantedAuthority> authorities; // 권한

    private AuthenticatedMember(String id, List<GrantedAuthority> authorities) {
        this.id = id;
        this.authorities = authorities;
    }

    /**
     * 토큰 클레임으로 인증된 회원을 생성합니다.
     *
     * @param claims 검증된 Access Token 클레임
     * @return 인증된 회원
     */
    public static AuthenticatedMember from(JwtClaims claims) {
        List<GrantedAuthority> authorities = claims.authorities().stream()
                .<GrantedAuthority>map(code -> new SimpleGrantedAuthority(ROLE_PREFIX + code))
                .toList();
        return new AuthenticatedMember(claims.memberId(), authorities);
    }

    /**
     * 토큰 기반 인증에는 비밀번호가 없습니다.
     *
     * @return null
     */
    @Override
    public String getPassword() {
        return null;
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
}
