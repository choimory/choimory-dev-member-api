package dev.choimory.member.api.common.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.choimory.member.api.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Security 설정 통합 테스트입니다. 필터를 켠 상태로 인증이 필요한 주소와 열려 있는 주소를 확인합니다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc; // MockMvc

    /**
     * 인증 없이 보호된 주소에 접근하면 공통 응답 JSON 형태의 401이어야 합니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void protectedPathWithoutAuthenticationIsUnauthorized() throws Exception {
        // 인증이 필요한 주소를 인증 없이 호출한다.
        mockMvc.perform(get("/v1/member/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.name").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    /**
     * CSRF를 사용하지 않으므로 인증 없는 POST, DELETE도 403이 아니라 401이어야 합니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void unsafeMethodsWithoutAuthenticationAreUnauthorizedNotForbidden() throws Exception {
        // CSRF 토큰 없이 인증이 필요한 주소를 POST, DELETE로 호출한다.
        mockMvc.perform(post("/v1/member/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/v1/member/me")).andExpect(status().isUnauthorized());
    }

    /**
     * 회원가입 주소는 인증 없이 열려 있어, 잘못된 요청이어도 401이 아니라 검증 실패 400이어야 합니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void joinPathIsOpenWithoutAuthentication() throws Exception {
        // 인증 없이 빈 요청 본문으로 가입 주소를 호출한다.
        mockMvc.perform(post("/v1/member/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * 회원가입 주소가 아닌 다른 HTTP 메서드는 열려 있지 않아 401이어야 합니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void joinPathOtherMethodsAreNotOpen() throws Exception {
        // 가입 주소를 GET으로 인증 없이 호출한다.
        mockMvc.perform(get("/v1/member/join")).andExpect(status().isUnauthorized());
    }
}
