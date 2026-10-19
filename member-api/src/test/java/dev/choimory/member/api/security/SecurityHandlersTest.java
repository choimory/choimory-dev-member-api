package dev.choimory.member.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.choimory.member.api.security.handler.CommonAccessDeniedHandler;
import dev.choimory.member.api.security.handler.CommonAuthenticationEntryPoint;
import dev.choimory.member.api.security.handler.SecurityResponseWriter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * Security 응답 핸들러 단위 테스트입니다. Docker 없이 응답 상태와 JSON 본문을 확인합니다.
 */
class SecurityHandlersTest {

    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON 변환
    private final SecurityResponseWriter writer = new SecurityResponseWriter(objectMapper); // 공통 응답 작성

    /**
     * 진입점은 인증 실패 사유를 노출하지 않고 401 공통 응답을 작성합니다.
     *
     * @throws Exception 응답 작성 중 발생
     */
    @Test
    void entryPointWritesUnauthorizedWithoutReason() throws Exception {
        // 인증 예외로 진입점을 호출한다.
        MockHttpServletResponse response = new MockHttpServletResponse();
        new CommonAuthenticationEntryPoint(writer)
                .commence(new MockHttpServletRequest(), response, new BadCredentialsException("secret reason"));

        // 401과 공통 응답 JSON이어야 하며 예외 메시지가 노출되면 안 된다.
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("code").asInt()).isEqualTo(401);
        assertThat(body.get("name").asText()).isEqualTo("UNAUTHORIZED");
        assertThat(body.get("message").asText()).isEqualTo("Unauthorized");
        assertThat(response.getContentAsString()).doesNotContain("secret reason");
    }

    /**
     * 접근 거부 핸들러는 403 공통 응답을 작성합니다.
     *
     * @throws Exception 응답 작성 중 발생
     */
    @Test
    void accessDeniedHandlerWritesForbidden() throws Exception {
        // 접근 거부 예외로 핸들러를 호출한다.
        MockHttpServletResponse response = new MockHttpServletResponse();
        new CommonAccessDeniedHandler(writer)
                .handle(new MockHttpServletRequest(), response, new AccessDeniedException("denied"));

        // 403과 공통 응답 JSON이어야 한다.
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/json");
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("code").asInt()).isEqualTo(403);
        assertThat(body.get("name").asText()).isEqualTo("FORBIDDEN");
        assertThat(body.get("message").asText()).isEqualTo("Forbidden");
    }

    /**
     * 한글이 포함된 값도 UTF-8로 작성됩니다.
     *
     * @throws Exception 응답 작성 중 발생
     */
    @Test
    void responseIsUtf8() throws Exception {
        // 응답을 작성한다.
        MockHttpServletResponse response = new MockHttpServletResponse();
        writer.write(response, org.springframework.http.HttpStatus.UNAUTHORIZED);

        // 문자 인코딩이 UTF-8이어야 한다.
        assertThat(response.getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");
    }
}
