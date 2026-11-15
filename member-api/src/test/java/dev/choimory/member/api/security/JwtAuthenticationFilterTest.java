package dev.choimory.member.api.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.choimory.member.api.TestcontainersConfiguration;
import dev.choimory.member.api.config.properties.JwtProperties;
import dev.choimory.member.api.jwt.v1.command.util.JwtProvider;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * JWT 인증 필터 통합 테스트입니다. 테스트용 컨트롤러로 토큰에 따른 인증, 인가 결과를 확인합니다.
 */
@Import({TestcontainersConfiguration.class, TestSecurityController.class})
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class JwtAuthenticationFilterTest {

    @Autowired
    private MockMvc mockMvc; // MockMvc

    @Autowired
    private JwtProvider jwtProvider; // JWT 생성과 검증

    @Autowired
    private JwtProperties jwtProperties; // JWT 설정

    /**
     * 유효한 Access Token이면 인증되어 토큰의 회원 ID와 권한이 principal에 채워집니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void validAccessTokenAuthenticates() throws Exception {
        // 유효한 Access Token으로 호출한다.
        mockMvc.perform(get("/test/whoami").header(HttpHeaders.AUTHORIZATION, bearer(access("member-1", "MEMBER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("member-1"))
                .andExpect(jsonPath("$[1]").value("ROLE_MEMBER"));
    }

    /**
     * 토큰이 없으면 일반 401(UNAUTHORIZED)이어야 합니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void missingTokenIsUnauthorized() throws Exception {
        // 토큰 없이 호출한다.
        mockMvc.perform(get("/test/whoami"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.name").value("UNAUTHORIZED"));
    }

    /**
     * Bearer가 아닌 방식의 Authorization 헤더는 토큰으로 보지 않고 일반 401이어야 합니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void nonBearerSchemeIsUnauthorized() throws Exception {
        // Basic 방식 헤더로 호출한다.
        mockMvc.perform(get("/test/whoami").header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.name").value("UNAUTHORIZED"));
    }

    /**
     * 만료된 토큰은 TOKEN_EXPIRED(401)로 응답해 클라이언트가 재발급할 수 있게 합니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void expiredTokenIsTokenExpired() throws Exception {
        // 이미 만료된 Access Token을 만든다.
        JwtProvider expiring = new JwtProvider(new JwtProperties(
                jwtProperties.secret(),
                jwtProperties.issuer(),
                Duration.ofSeconds(-10),
                jwtProperties.refreshExpiration()));
        String expired =
                expiring.createAccessToken("member-1", List.of("MEMBER")).token();

        // 만료 토큰으로 호출하면 TOKEN_EXPIRED여야 한다.
        mockMvc.perform(get("/test/whoami").header(HttpHeaders.AUTHORIZATION, bearer(expired)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.name").value("TOKEN_EXPIRED"));
    }

    /**
     * 잘못된 토큰과 Access 자리의 Refresh Token은 INVALID_TOKEN(401)입니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void invalidAndRefreshTokensAreInvalidToken() throws Exception {
        // 아무 문자열과 Refresh Token으로 호출한다.
        for (String token :
                List.of("not-a-jwt", jwtProvider.createRefreshToken("member-1").token())) {
            mockMvc.perform(get("/test/whoami").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.name").value("INVALID_TOKEN"));
        }
    }

    /**
     * 권한이 없으면 403(FORBIDDEN), 권한이 있으면 통과해야 합니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void roleBasedAuthorization() throws Exception {
        // MEMBER 권한만 있는 토큰으로 ADMIN 전용 주소를 호출하면 403이어야 한다.
        mockMvc.perform(get("/test/admin").header(HttpHeaders.AUTHORIZATION, bearer(access("member-1", "MEMBER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.name").value("FORBIDDEN"));

        // ADMIN 권한이 있는 토큰으로는 통과해야 한다.
        mockMvc.perform(get("/test/admin").header(HttpHeaders.AUTHORIZATION, bearer(access("admin-1", "ADMIN"))))
                .andExpect(status().isOk());
    }

    /**
     * 인증이 필요 없는 주소(회원가입)는 토큰이 잘못되어도 401이 아니라 그대로 처리됩니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void permitAllPathIgnoresInvalidToken() throws Exception {
        // 잘못된 토큰과 빈 요청 본문으로 가입 주소를 호출하면 요청 값 검증 실패 400이어야 한다.
        mockMvc.perform(post("/v1/member/join")
                        .header(HttpHeaders.AUTHORIZATION, bearer("not-a-jwt"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Access Token을 발급합니다.
     *
     * @param memberId 회원 ID
     * @param authority 권한 코드
     * @return 토큰 문자열
     */
    private String access(String memberId, String authority) {
        return jwtProvider.createAccessToken(memberId, List.of(authority)).token();
    }

    /**
     * Authorization 헤더 값을 만듭니다.
     *
     * @param token 토큰 문자열
     * @return Bearer 헤더 값
     */
    private String bearer(String token) {
        return "Bearer " + token;
    }
}
