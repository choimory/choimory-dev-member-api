package dev.choimory.member.api.member.v1.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.choimory.member.api.TestcontainersConfiguration;
import dev.choimory.member.api.config.properties.JwtProperties;
import dev.choimory.member.api.jwt.v1.command.payload.dto.JwtClaims;
import dev.choimory.member.api.jwt.v1.command.payload.type.TokenType;
import dev.choimory.member.api.jwt.v1.command.repository.JwtRepository;
import dev.choimory.member.api.jwt.v1.command.util.JwtProvider;
import dev.choimory.member.api.member.v1.command.payload.request.MemberJoinRequest;
import dev.choimory.member.api.security.TestSecurityController;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 토큰 재발급 API(POST /v1/member/reissue) 통합 테스트입니다. 실제 PostgreSQL과 Redis(Testcontainers)로 가입, 로그인, 재발급을 확인합니다.
 */
@Import({TestcontainersConfiguration.class, TestSecurityController.class})
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class MemberReissueTest {

    private static final String PASSWORD = "Password1!"; // 테스트 회원 비밀번호

    @Autowired
    private MockMvc mockMvc; // MockMvc

    @Autowired
    private ObjectMapper objectMapper; // JSON 변환

    @Autowired
    private JdbcTemplate jdbc; // JDBC 템플릿

    @Autowired
    private StringRedisTemplate redisTemplate; // Redis 접근

    @Autowired
    private JwtProvider jwtProvider; // JWT 검증

    @Autowired
    private JwtProperties jwtProperties; // JWT 설정

    @Autowired
    private JwtRepository jwtRepository; // JWT 데이터 접근 객체

    /**
     * 테스트 데이터를 정리합니다. 권한 기본 데이터(V3)는 남겨 둡니다.
     */
    @BeforeEach
    @AfterEach
    void cleanUp() {
        jdbc.execute("truncate table member_authority, member cascade");
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
    }

    /**
     * 재발급에 성공하면 새 토큰이 내려오고 이전 Refresh Token은 삭제되며 새 Refresh Token이 저장됩니다. (rotation)
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void reissueRotatesRefreshToken() throws Exception {
        // 가입하고 로그인해 토큰을 받는다.
        join("a@a.com", "nick1");
        JsonNode first = loginData("a@a.com");
        String memberId = jdbc.queryForObject("select id from member where email = 'a@a.com'", String.class);
        JwtClaims oldRefresh = jwtProvider.parse(first.get("refreshToken").asText(), TokenType.REFRESH);

        // 재발급을 요청한다.
        String body = reissue(first.get("refreshToken").asText())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode second = objectMapper.readTree(body).get("data");

        // 새 토큰은 이전 토큰과 달라야 한다.
        assertThat(second.get("accessToken").asText())
                .isNotEqualTo(first.get("accessToken").asText());
        assertThat(second.get("refreshToken").asText())
                .isNotEqualTo(first.get("refreshToken").asText());

        // 이전 Refresh Token은 삭제되고 새 Refresh Token만 저장되어 있어야 한다.
        JwtClaims newRefresh = jwtProvider.parse(second.get("refreshToken").asText(), TokenType.REFRESH);
        assertThat(jwtRepository.existsRefreshToken(memberId, oldRefresh.jti())).isFalse();
        assertThat(jwtRepository.existsRefreshToken(memberId, newRefresh.jti())).isTrue();

        // 새 Access Token으로 인증이 필요한 주소를 호출할 수 있어야 한다.
        mockMvc.perform(get("/test/whoami")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + second.get("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(memberId));
    }

    /**
     * 한 번 쓴 Refresh Token으로 다시 재발급하면 INVALID_TOKEN(401)이어야 합니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void usedRefreshTokenCannotBeReused() throws Exception {
        // 재발급을 한 번 성공한다.
        join("a@a.com", "nick1");
        String refreshToken = loginData("a@a.com").get("refreshToken").asText();
        reissue(refreshToken).andExpect(status().isOk());

        // 같은 Refresh Token으로 다시 재발급하면 거부되어야 한다.
        reissue(refreshToken)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.name").value("INVALID_TOKEN"));
    }

    /**
     * 새 Access Token에는 회원의 현재 권한이 반영됩니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void reissuedAccessTokenReflectsCurrentAuthorities() throws Exception {
        // 가입하고 로그인한다. 이때의 권한은 MEMBER뿐이다.
        join("a@a.com", "nick1");
        JsonNode first = loginData("a@a.com");
        JwtClaims before = jwtProvider.parse(first.get("accessToken").asText(), TokenType.ACCESS);
        assertThat(before.authorities()).containsExactly("MEMBER");

        // 로그인 이후 회원에게 ADMIN 권한이 추가된다.
        jdbc.update(
                "insert into member_authority (id, member_id, authority_id, created_at)"
                        + " select ?, m.id, a.id, now() from member m, authority a"
                        + " where m.email = 'a@a.com' and a.code = 'ADMIN'",
                java.util.UUID.randomUUID().toString());

        // 재발급하면 새 Access Token에 두 권한이 모두 있어야 한다.
        String body = reissue(first.get("refreshToken").asText())
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JwtClaims after = jwtProvider.parse(
                objectMapper.readTree(body).get("data").get("accessToken").asText(), TokenType.ACCESS);
        assertThat(after.authorities()).containsExactlyInAnyOrder("MEMBER", "ADMIN");
    }

    /**
     * 로그인할 수 없는 상태가 되었거나 탈퇴한 회원은 재발급할 수 없고, 새 Refresh Token도 저장되지 않아야 합니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void disabledOrDeletedMemberCannotReissue() throws Exception {
        join("a@a.com", "nick1");

        // 로그인 후 회원이 정지 상태가 되면 재발급할 수 없다.
        String suspended = loginData("a@a.com").get("refreshToken").asText();
        jdbc.update("update member set status = 'SUSPENDED' where email = 'a@a.com'");
        reissue(suspended)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.name").value("INVALID_TOKEN"));

        // 다시 활성 상태로 돌려 로그인한 뒤 회원이 탈퇴(soft delete)하면 재발급할 수 없다.
        jdbc.update("update member set status = 'ACTIVATE' where email = 'a@a.com'");
        String deleted = loginData("a@a.com").get("refreshToken").asText();
        jdbc.update("update member set deleted_at = now() where email = 'a@a.com'");
        reissue(deleted)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.name").value("INVALID_TOKEN"));

        // 실패한 재발급은 새 Refresh Token을 남기지 않아야 한다. (이전 토큰은 사용 처리되어 모두 삭제됨)
        assertThat(redisTemplate.keys("refresh:*")).isEmpty();
    }

    /**
     * 만료된 Refresh Token은 TOKEN_EXPIRED, 잘못된 토큰과 Access Token은 INVALID_TOKEN, 저장되지 않은 토큰은 INVALID_TOKEN입니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void invalidRefreshTokensAreRejected() throws Exception {
        join("a@a.com", "nick1");
        JsonNode login = loginData("a@a.com");
        String memberId = jdbc.queryForObject("select id from member where email = 'a@a.com'", String.class);

        // 만료된 Refresh Token은 TOKEN_EXPIRED여야 한다.
        JwtProvider expiring = new JwtProvider(new JwtProperties(
                jwtProperties.secret(),
                jwtProperties.issuer(),
                jwtProperties.accessExpiration(),
                Duration.ofSeconds(-10)));
        reissue(expiring.createRefreshToken(memberId).token())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.name").value("TOKEN_EXPIRED"));

        // 아무 문자열과 Access Token은 INVALID_TOKEN이어야 한다.
        reissue("not-a-jwt")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.name").value("INVALID_TOKEN"));
        reissue(login.get("accessToken").asText())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.name").value("INVALID_TOKEN"));

        // 서명은 맞지만 Redis에 저장된 적 없는(또는 폐기된) Refresh Token은 INVALID_TOKEN이어야 한다.
        reissue(jwtProvider.createRefreshToken(memberId).token())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.name").value("INVALID_TOKEN"));
    }

    /**
     * Refresh Token이 비어 있으면 400과 실패한 필드 목록이, JSON이 아닌 본문은 400이어야 합니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void blankOrMalformedRequestIsBadRequest() throws Exception {
        // 필드가 없거나 공백이면 실패한 필드 목록이 내려와야 한다.
        mockMvc.perform(post("/v1/member/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[*].field").value(hasItems("refreshToken")));
        reissue("  ").andExpect(status().isBadRequest());

        // JSON이 아닌 본문은 500이 아니라 400이어야 한다.
        mockMvc.perform(post("/v1/member/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("BAD_REQUEST"));
    }

    /**
     * 재발급 주소는 POST만 열려 있고, 인증 없이도 호출할 수 있어야 합니다. (잘못된 Access Token을 보내도 재발급은 처리됨)
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void reissueIsOpenWithoutAuthentication() throws Exception {
        join("a@a.com", "nick1");
        String refreshToken = loginData("a@a.com").get("refreshToken").asText();

        // 만료·잘못된 Access Token을 Authorization 헤더에 담아 보내도 재발급은 처리되어야 한다.
        mockMvc.perform(post("/v1/member/reissue")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk());

        // GET은 열려 있지 않아 401이어야 한다.
        mockMvc.perform(get("/v1/member/reissue")).andExpect(status().isUnauthorized());
    }

    /**
     * 회원가입 API로 회원을 만듭니다.
     *
     * @param email 이메일
     * @param nickname 닉네임
     * @throws Exception MockMvc 수행 중 발생
     */
    private void join(String email, String nickname) throws Exception {
        MemberJoinRequest request = MemberJoinRequest.builder()
                .email(email)
                .password(PASSWORD)
                .nickname(nickname)
                .build();
        mockMvc.perform(post("/v1/member/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    /**
     * 로그인하고 응답의 data(토큰)를 반환합니다.
     *
     * @param email 이메일
     * @return 로그인 응답의 data
     * @throws Exception MockMvc 수행 중 발생
     */
    private JsonNode loginData(String email) throws Exception {
        String body = mockMvc.perform(post("/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).get("data");
    }

    /**
     * 재발급 요청을 수행합니다.
     *
     * @param refreshToken Refresh Token
     * @return 수행 결과
     * @throws Exception MockMvc 수행 중 발생
     */
    private ResultActions reissue(String refreshToken) throws Exception {
        return mockMvc.perform(post("/v1/member/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))));
    }
}
