package dev.choimory.member.api.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.choimory.member.domain.common.command.response.CommonResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Security 필터 단계의 응답을 공통 응답 JSON으로 작성하는 컴포넌트입니다.
 * 필터에서 발생한 예외는 {@code @RestControllerAdvice}에 닿지 않으므로 응답을 직접 작성합니다.
 */
@Component
@RequiredArgsConstructor
public class SecurityResponseWriter {

    private final ObjectMapper objectMapper; // JSON 변환

    /**
     * HTTP 상태의 기본값으로 공통 응답(data 없음)을 작성합니다.
     *
     * @param response HTTP 응답
     * @param status HTTP 상태
     * @throws IOException 응답을 쓰지 못한 경우
     */
    public void write(HttpServletResponse response, HttpStatus status) throws IOException {
        // 상태의 기본값으로 공통 응답을 만든다.
        CommonResponse<String> body = new CommonResponse<>(status.value(), status.name(), status.getReasonPhrase());

        // 상태와 JSON 본문을 응답에 쓴다.
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
