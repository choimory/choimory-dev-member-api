package dev.choimory.member.api.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.choimory.member.api.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * CORS 통합 테스트입니다. 필터를 켠 상태에서 preflight와 실제 요청의 CORS 헤더를 확인합니다.
 * local 프로파일의 허용 Origin은 http://localhost:3000 하나입니다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class CorsTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:3000"; // local 허용 Origin
    private static final String DISALLOWED_ORIGIN = "https://evil.example.com"; // 허용하지 않는 Origin

    @Autowired
    private MockMvc mockMvc; // MockMvc

    /**
     * 허용한 Origin의 preflight는 인증 없이 통과하고 허용 메서드, 헤더, 캐시 시간이 응답에 담겨야 합니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void preflightFromAllowedOriginIsOk() throws Exception {
        // 인증이 필요한 주소에 대해 허용된 Origin이 preflight를 보낸다.
        mockMvc.perform(options("/v1/member/me")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                .andExpect(
                        header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,PATCH,DELETE,OPTIONS"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Authorization, Content-Type"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    /**
     * 허용하지 않은 Origin의 preflight는 403이고 CORS 허용 헤더가 없어야 합니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void preflightFromDisallowedOriginIsForbidden() throws Exception {
        // 허용하지 않은 Origin이 preflight를 보낸다.
        mockMvc.perform(options("/v1/member/me")
                        .header(HttpHeaders.ORIGIN, DISALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    /**
     * 허용하지 않은 메서드와 헤더를 요청하는 preflight는 403이어야 합니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void preflightWithDisallowedMethodOrHeaderIsForbidden() throws Exception {
        // 허용하지 않은 메서드(TRACE)로 preflight를 보낸다.
        mockMvc.perform(options("/v1/member/me")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "TRACE"))
                .andExpect(status().isForbidden());

        // 허용하지 않은 헤더(X-Custom)를 요청하는 preflight를 보낸다.
        mockMvc.perform(options("/v1/member/me")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "X-Custom"))
                .andExpect(status().isForbidden());
    }

    /**
     * 인증 실패(401) 응답에도 CORS 헤더가 붙어야 브라우저가 응답 본문을 읽을 수 있습니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void unauthorizedResponseHasCorsHeaders() throws Exception {
        // 허용된 Origin이 인증 없이 보호된 주소를 호출한다.
        mockMvc.perform(get("/v1/member/me").header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Authorization"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    /**
     * 허용하지 않은 Origin의 실제 요청은 CORS 검증에서 403으로 거부되어야 합니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void actualRequestFromDisallowedOriginIsForbidden() throws Exception {
        // 허용하지 않은 Origin이 열려 있는 주소를 호출한다.
        mockMvc.perform(get("/v1/member/me").header(HttpHeaders.ORIGIN, DISALLOWED_ORIGIN))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
