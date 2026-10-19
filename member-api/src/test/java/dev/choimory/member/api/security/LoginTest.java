package dev.choimory.member.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.choimory.member.api.TestcontainersConfiguration;
import dev.choimory.member.api.jwt.JwtClaims;
import dev.choimory.member.api.jwt.JwtProvider;
import dev.choimory.member.api.jwt.TokenType;
import dev.choimory.member.api.member.v1.command.payload.request.MemberJoinRequest;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 로그인 API 통합 테스트입니다. 로그인 필터가 처리하며, 실제 PostgreSQL과 Redis(Testcontainers)로 가입부터 로그인, 토큰 사용까지 확인합니다.
 */
@Import({TestcontainersConfiguration.class, TestSecurityController.class})
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class LoginTest {

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

    /**
     * 테스트 데이터를 정리합니다. 권한 기본 데이터(V3)는 남겨 둡니다.
     */
    @BeforeEach
    @AfterEach
    void cleanUp() {
        jdbc.execute("truncate table member_authority, member cascade");
        redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
    }

    /**
     * 로그인에 성공하면 두 토큰이 내려오고 Access Token에 회원 ID와 권한이 담기며 Refresh Token이 Redis에 저장됩니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void loginIssuesTokensAndStoresRefreshToken() throws Exception {
        // 가입한 회원으로 로그인한다.
        join("a@a.com", "nick1");
        MvcResult result = login("a@a.com", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.accessExpiresIn").value(org.hamcrest.Matchers.greaterThan(1790)))
                .andReturn();
        JsonNode data =
                objectMapper.readTree(result.getResponse().getContentAsString()).get("data");

        // Access Token에는 회원 ID와 MEMBER 권한이 담겨야 한다.
        JwtClaims access = jwtProvider.parse(data.get("accessToken").asText(), TokenType.ACCESS);
        String memberId = jdbc.queryForObject("select id from member where email = 'a@a.com'", String.class);
        assertThat(access.memberId()).isEqualTo(memberId);
        assertThat(access.authorities()).containsExactly("MEMBER");

        // Refresh Token은 Redis에 refresh:{memberId}:{jti} 키로 14일 안팎의 TTL로 저장되어야 한다.
        JwtClaims refresh = jwtProvider.parse(data.get("refreshToken").asText(), TokenType.REFRESH);
        String key = "refresh:" + memberId + ":" + refresh.jti();
        assertThat(redisTemplate.hasKey(key)).isTrue();
        assertThat(redisTemplate.getExpire(key))
                .isBetween(
                        Duration.ofDays(14).getSeconds() - 10,
                        Duration.ofDays(14).getSeconds());
    }

    /**
     * 같은 회원이 여러 번 로그인하면 기기별로 Refresh Token이 각각 저장됩니다. (다중 로그인)
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void multipleLoginsKeepEachRefreshToken() throws Exception {
        // 같은 회원이 두 번 로그인한다.
        join("a@a.com", "nick1");
        login("a@a.com", PASSWORD).andExpect(status().isOk());
        login("a@a.com", PASSWORD).andExpect(status().isOk());

        // Refresh Token이 두 개 저장되어야 한다.
        String memberId = jdbc.queryForObject("select id from member where email = 'a@a.com'", String.class);
        Set<String> keys = redisTemplate.keys("refresh:" + memberId + ":*");
        assertThat(keys).hasSize(2);
    }

    /**
     * 로그인으로 받은 Access Token으로 인증이 필요한 주소를 호출할 수 있습니다.
     * /test/whoami는 테스트용 컨트롤러(TestSecurityController)를 import해서 사용합니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void accessTokenFromLoginAuthenticatesRequests() throws Exception {
        // 로그인해 Access Token을 받는다.
        join("a@a.com", "nick1");
        String body = login("a@a.com", PASSWORD).andReturn().getResponse().getContentAsString();
        String accessToken =
                objectMapper.readTree(body).get("data").get("accessToken").asText();

        // 받은 토큰으로 인증이 필요한 주소를 호출하면 성공해야 한다.
        mockMvc.perform(get("/test/whoami").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1]").value("ROLE_MEMBER"));
    }

    /**
     * 틀린 비밀번호, 없는 이메일은 같은 401 응답이어야 합니다. (계정 존재 여부 노출 방지)
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void wrongPasswordAndUnknownEmailGetSameUnauthorized() throws Exception {
        join("a@a.com", "nick1");

        // 틀린 비밀번호와 없는 이메일로 로그인한다.
        String wrongPassword = login("a@a.com", "Wrong1234!")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.name").value("UNAUTHORIZED"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String unknownEmail = login("none@a.com", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 응답 본문이 완전히 같아야 한다.
        assertThat(wrongPassword).isEqualTo(unknownEmail);
        assertThat(redisTemplate.keys("refresh:*")).isEmpty();
    }

    /**
     * ACTIVATE가 아닌 상태(가입 대기, 비활성, 정지)의 회원은 올바른 비밀번호여도 같은 401이어야 합니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void nonActivateMembersCannotLogin() throws Exception {
        join("a@a.com", "nick1");

        // 가입 대기, 비활성, 정지 상태일 때 로그인한다.
        for (String memberStatus : new String[] {"REGISTER", "INACTIVATE", "SUSPENDED"}) {
            jdbc.update("update member set status = ? where email = 'a@a.com'", memberStatus);
            login("a@a.com", PASSWORD)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.name").value("UNAUTHORIZED"));
        }

        // 다시 활성 상태로 돌리면 로그인할 수 있어야 한다.
        jdbc.update("update member set status = 'ACTIVATE' where email = 'a@a.com'");
        login("a@a.com", PASSWORD).andExpect(status().isOk());
    }

    /**
     * 탈퇴(soft delete)한 회원은 로그인할 수 없습니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void softDeletedMemberCannotLogin() throws Exception {
        // 가입 후 회원을 soft delete 한다.
        join("a@a.com", "nick1");
        jdbc.update("update member set deleted_at = now() where email = 'a@a.com'");

        // 로그인하면 401이어야 한다.
        login("a@a.com", PASSWORD).andExpect(status().isUnauthorized());
    }

    /**
     * 요청 본문이 JSON이 아니거나 이메일, 비밀번호가 비어 있으면 인증을 시도하지 않고 400이어야 합니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void malformedOrBlankRequestIsBadRequest() throws Exception {
        // JSON이 아닌 본문, 빈 객체, 이메일만 있는 본문, 비밀번호가 공백인 본문, 빈 본문으로 로그인한다.
        for (String body : new String[] {
            "not-json", "{}", "{\"email\":\"a@a.com\"}", "{\"email\":\"a@a.com\",\"password\":\"  \"}", ""
        }) {
            mockMvc.perform(post("/v1/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.name").value("BAD_REQUEST"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
        assertThat(redisTemplate.keys("refresh:*")).isEmpty();
    }

    /**
     * 로그인 주소는 POST만 처리하고, 다른 메서드는 인증이 필요한 주소로 취급되어 401이어야 합니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void getOnLoginPathIsUnauthorized() throws Exception {
        mockMvc.perform(get("/v1/login")).andExpect(status().isUnauthorized());
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
     * 로그인 요청을 수행합니다.
     *
     * @param email 이메일
     * @param password 비밀번호
     * @return 수행 결과
     * @throws Exception MockMvc 수행 중 발생
     */
    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("email", email, "password", password))));
    }
}
