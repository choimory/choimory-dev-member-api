package dev.choimory.member.api.security;

import dev.choimory.member.api.security.user.AuthenticatedMember;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증, 인가 결과를 확인하기 위한 테스트용 컨트롤러입니다. 필요한 테스트가 {@code @Import}해서 사용합니다.
 * 테스트 클래스의 중첩 클래스는 컴포넌트 스캔에서 제외되므로 별도 클래스로 둡니다.
 */
@RestController
public class TestSecurityController {

    /**
     * 인증된 회원의 ID와 첫 번째 권한을 반환합니다.
     *
     * @param member 인증된 회원
     * @return 회원 ID와 첫 번째 권한
     */
    @GetMapping("/test/whoami")
    List<String> whoami(@AuthenticationPrincipal AuthenticatedMember member) {
        return List.of(member.getId(), member.getAuthorities().iterator().next().getAuthority());
    }

    /**
     * ADMIN 권한이 있어야 호출할 수 있습니다.
     *
     * @return ok
     */
    @GetMapping("/test/admin")
    @PreAuthorize("hasRole('ADMIN')")
    String admin() {
        return "ok";
    }
}
