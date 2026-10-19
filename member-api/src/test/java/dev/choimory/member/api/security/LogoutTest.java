package dev.choimory.member.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.choimory.member.api.TestcontainersConfiguration;
import dev.choimory.member.api.jwt.v1.command.payload.dto.JwtClaims;
import dev.choimory.member.api.jwt.v1.command.payload.dto.JwtProperties;
import dev.choimory.member.api.jwt.v1.command.payload.type.TokenType;
import dev.choimory.member.api.jwt.v1.command.util.JwtProvider;
import dev.choimory.member.api.member.v1.command.payload.request.MemberJoinRequest;
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
 * 로그아웃 API(POST /v1/logout) 통합 테스트입니다. 실제 PostgreSQL과 Redis(Testcontainers)로
 * 가입, 로그인, 로그아웃과 그 이후의 토큰 사용을 확인합니다.
 */
@Import({TestcontainersConfiguration.class, TestSecurityController.class})
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class LogoutTest {

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
     * 로그아웃하면 Refresh Token이 폐기되고 Access Token은 블랙리스트에 올라 더 이상 쓸 수 없습니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void logoutRevokesRefreshTokenAndBlacklistsAccessToken() throws Exception {
        join("a@a.com", "nick1");
        JsonNode tokens = loginData("a@a.com");
        String access = tokens.get("accessToken").asText();
        String refresh = tokens.get("refreshToken").asText();
        JwtClaims accessClaims = jwtProvider.parse(access, TokenType.ACCESS);

        // 로그아웃 전에는 Access Token으로 인증이 필요한 주소를 호출할 수 있다.
        mockMvc.perform(get("/test/whoami").header(HttpHeaders.AUTHORIZATION, "Bearer " + access))
                .andExpect(status().isOk());

        // Access Token을 헤더에 담아 로그아웃한다.
        logout(refresh, access)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.name").value("OK"))
                .andExpect(jsonPath("$.data").doesNotExist());

        // Refresh Token은 삭제되고 Access Token의 jti는 남은 만료 시간만큼 블랙리스트에 올라가야 한다.
        assertThat(redisTemplate.keys("refresh:*")).isEmpty();
        assertThat(redisTemplate.hasKey("blacklist:" + accessClaims.jti())).isTrue();
        assertThat(redisTemplate.getExpire("blacklist:" + accessClaims.jti()))
                .isBetween(
                        Duration.ofMinutes(30).getSeconds() - 10,
                        Duration.ofMinutes(30).getSeconds());

        // 로그아웃한 Access Token은 더 이상 쓸 수 없고, Refresh Token으로 재발급도 할 수 없다.
        mockMvc.perform(get("/test/whoami").header(HttpHeaders.AUTHORIZATION, "Bearer " + access))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.name").value("INVALID_TOKEN"));
        reissue(refresh)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.name").value("INVALID_TOKEN"));
    }

    /**
     * 한 기기에서 로그아웃해도 같은 회원의 다른 기기 Refresh Token은 그대로입니다. (다중 로그인)
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void logoutKeepsOtherDevicesRefreshToken() throws Exception {
        // 같은 회원이 두 기기에서 로그인한다.
        join("a@a.com", "nick1");
        JsonNode device1 = loginData("a@a.com");
        JsonNode device2 = loginData("a@a.com");

        // 첫 번째 기기에서만 로그아웃한다.
        logout(device1.get("refreshToken").asText(), device1.get("accessToken").asText())
                .andExpect(status().isOk());

        // 두 번째 기기의 Refresh Token과 Access Token은 계속 쓸 수 있어야 한다.
        assertThat(redisTemplate.keys("refresh:*")).hasSize(1);
        mockMvc.perform(get("/test/whoami")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + device2.get("accessToken").asText()))
                .andExpect(status().isOk());
        reissue(device2.get("refreshToken").asText()).andExpect(status().isOk());
    }

    /**
     * Authorization 헤더가 없거나 만료·잘못된 Access Token이 있어도 Refresh Token만으로 로그아웃할 수 있습니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void logoutWorksWithoutValidAccessToken() throws Exception {
        join("a@a.com", "nick1");

        // 헤더 없이 로그아웃하면 Refresh Token만 폐기되고 블랙리스트는 생기지 않는다.
        JsonNode first = loginData("a@a.com");
        logout(first.get("refreshToken").asText(), null).andExpect(status().isOk());
        assertThat(redisTemplate.keys("refresh:*")).isEmpty();
        assertThat(redisTemplate.keys("blacklist:*")).isEmpty();

        // 잘못된 Access Token을 담아도 로그아웃된다.
        JsonNode second = loginData("a@a.com");
        logout(second.get("refreshToken").asText(), "not-a-jwt").andExpect(status().isOk());
        assertThat(redisTemplate.keys("refresh:*")).isEmpty();

        // 이미 만료된 Access Token을 담아도 로그아웃되고, 만료된 토큰은 블랙리스트에 올리지 않는다.
        JwtProvider expiring = new JwtProvider(new JwtProperties(
                jwtProperties.secret(),
                jwtProperties.issuer(),
                Duration.ofSeconds(-10),
                jwtProperties.refreshExpiration()));
        JsonNode third = loginData("a@a.com");
        String memberId = jdbc.queryForObject("select id from member where email = 'a@a.com'", String.class);
        logout(
                        third.get("refreshToken").asText(),
                        expiring.createAccessToken(memberId, java.util.List.of("MEMBER"))
                                .token())
                .andExpect(status().isOk());
        assertThat(redisTemplate.keys("refresh:*")).isEmpty();
        assertThat(redisTemplate.keys("blacklist:*")).isEmpty();
    }

    /**
     * 다른 회원의 Access Token을 함께 보내도 그 토큰은 블랙리스트에 오르지 않고 Refresh Token만 폐기됩니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void otherMembersAccessTokenIsNotBlacklisted() throws Exception {
        // 두 회원이 각각 로그인한다.
        join("a@a.com", "nick1");
        join("b@b.com", "nick2");
        JsonNode memberA = loginData("a@a.com");
        JsonNode memberB = loginData("b@b.com");

        // A의 Refresh Token과 B의 Access Token으로 로그아웃한다.
        logout(memberA.get("refreshToken").asText(), memberB.get("accessToken").asText())
                .andExpect(status().isOk());

        // A의 Refresh Token은 폐기되지만 B의 Access Token과 Refresh Token은 그대로여야 한다.
        assertThat(redisTemplate.keys("refresh:*")).hasSize(1);
        assertThat(redisTemplate.keys("blacklist:*")).isEmpty();
        mockMvc.perform(get("/test/whoami")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + memberB.get("accessToken").asText()))
                .andExpect(status().isOk());
    }

    /**
     * 같은 Refresh Token으로 다시 로그아웃해도 200입니다. (멱등)
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void logoutIsIdempotent() throws Exception {
        join("a@a.com", "nick1");
        String refresh = loginData("a@a.com").get("refreshToken").asText();

        // 두 번 연속 로그아웃해도 모두 200이어야 한다.
        logout(refresh, null).andExpect(status().isOk());
        logout(refresh, null).andExpect(status().isOk());
    }

    /**
     * 만료된 Refresh Token은 TOKEN_EXPIRED, 잘못된 문자열과 Access Token은 INVALID_TOKEN(401)이어야 합니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void invalidRefreshTokensAreUnauthorized() throws Exception {
        join("a@a.com", "nick1");
        JsonNode tokens = loginData("a@a.com");
        String memberId = jdbc.queryForObject("select id from member where email = 'a@a.com'", String.class);

        // 만료된 Refresh Token은 TOKEN_EXPIRED여야 한다.
        JwtProvider expiring = new JwtProvider(new JwtProperties(
                jwtProperties.secret(),
                jwtProperties.issuer(),
                jwtProperties.accessExpiration(),
                Duration.ofSeconds(-10)));
        logout(expiring.createRefreshToken(memberId).token(), null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.name").value("TOKEN_EXPIRED"));

        // 아무 문자열과 Access Token은 INVALID_TOKEN이어야 하고, 아무것도 폐기되지 않아야 한다.
        logout("not-a-jwt", null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.name").value("INVALID_TOKEN"));
        logout(tokens.get("accessToken").asText(), tokens.get("accessToken").asText())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.name").value("INVALID_TOKEN"));
        assertThat(redisTemplate.keys("refresh:*")).hasSize(1);
        assertThat(redisTemplate.keys("blacklist:*")).isEmpty();
    }

    /**
     * 요청 본문이 JSON이 아니거나 Refresh Token이 비어 있으면 400이며, GET은 열려 있지 않아 401입니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void malformedOrBlankRequestIsBadRequest() throws Exception {
        // JSON이 아닌 본문, 빈 객체, 공백, 빈 본문은 data 없는 400이어야 한다.
        for (String body : new String[] {"not-json", "{}", "{\"refreshToken\":\"  \"}", ""}) {
            mockMvc.perform(post("/v1/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.name").value("BAD_REQUEST"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }

        // GET은 로그아웃 요청으로 처리되지 않고 인증이 필요한 주소로 취급되어 401이다.
        mockMvc.perform(get("/v1/logout")).andExpect(status().isUnauthorized());
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
     * 로그아웃 요청을 수행합니다.
     *
     * @param refreshToken Refresh Token
     * @param accessToken Authorization 헤더에 담을 토큰, 헤더를 보내지 않으면 null
     * @return 수행 결과
     * @throws Exception MockMvc 수행 중 발생
     */
    private ResultActions logout(String refreshToken, String accessToken) throws Exception {
        var request = post("/v1/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken)));
        if (accessToken != null) {
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }
        return mockMvc.perform(request);
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
