package dev.choimory.member.api.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * CommonExceptionHandler 단위 테스트입니다. 예외를 던지는 테스트용 컨트롤러로 예외별 응답을 확인합니다.
 */
class CommonExceptionHandlerTest {

    private MockMvc mockMvc; // 테스트용 컨트롤러와 예외 핸들러만 올린 MockMvc

    /**
     * 테스트용 컨트롤러와 예외 핸들러로 MockMvc를 구성합니다.
     */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new CommonExceptionHandler())
                .build();
    }

    /**
     * 처리되지 않은 예외는 500 응답으로 변환됩니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void exceptionBecomesInternalServerError() throws Exception {
        // 체크 예외를 던지면 500 응답이어야 한다.
        mockMvc.perform(get("/exception"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.name").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("Internal Server Error"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    /**
     * RuntimeException은 Exception 핸들러를 거쳐 500 응답으로 변환되며 예외 메시지를 노출하지 않습니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void runtimeExceptionBecomesInternalServerError() throws Exception {
        // 런타임 예외를 던지면 500 응답이어야 한다.
        mockMvc.perform(get("/runtime"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    /**
     * CommonException은 예외에 지정된 상태와 값으로 변환됩니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void commonExceptionUsesGivenStatusAndValues() throws Exception {
        // 상태와 세부 값을 지정한 예외를 던지면 지정한 값으로 응답해야 한다.
        mockMvc.perform(get("/common"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.name").value("DUPLICATE_EMAIL"))
                .andExpect(jsonPath("$.message").value("중복 이메일"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    /**
     * 상태만 지정한 CommonException은 상태의 기본값으로 변환됩니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void commonExceptionWithStatusOnlyUsesDefaults() throws Exception {
        // 상태만 지정한 예외를 던지면 상태의 기본값으로 응답해야 한다.
        mockMvc.perform(get("/common-status"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.name").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Not Found"));
    }

    /**
     * Request body 검증 실패는 400 응답과 검증 실패 목록으로 변환됩니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void methodArgumentNotValidBecomesBadRequestWithResults() throws Exception {
        // 검증에 실패하는 요청 본문을 보내면 400 응답과 실패 목록이어야 한다.
        mockMvc.perform(post("/valid").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.name").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.data[0].field").value("name"))
                .andExpect(jsonPath("$.data[0].rejectValue").value(""))
                .andExpect(jsonPath("$.data[0].message").isNotEmpty());
    }

    /**
     * 제약 조건 위반은 400 응답과 검증 실패 목록으로 변환됩니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void constraintViolationBecomesBadRequestWithResults() throws Exception {
        // 제약 조건 위반 예외를 던지면 400 응답과 실패 목록이어야 한다.
        mockMvc.perform(get("/constraint"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data[0].field").value("name"))
                .andExpect(jsonPath("$.data[0].rejectValue").value(""))
                .andExpect(jsonPath("$.data[0].message").isNotEmpty());
    }

    /**
     * 인증 예외는 401 응답으로 변환되며 예외 메시지를 노출하지 않습니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void authenticationExceptionBecomesUnauthorized() throws Exception {
        // 인증 예외를 던지면 401 응답이어야 한다.
        mockMvc.perform(get("/auth"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.name").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    /**
     * 요청 본문 검증 대상 객체입니다.
     */
    record ValidRequest(@NotBlank String name) {}

    /**
     * 예외를 던지는 테스트용 컨트롤러입니다.
     */
    @RestController
    static class ThrowingController {

        @GetMapping("/exception")
        void exception() throws Exception {
            throw new Exception("checked");
        }

        @GetMapping("/runtime")
        void runtime() {
            throw new IllegalStateException("runtime");
        }

        @GetMapping("/common")
        void common() {
            throw new CommonException(HttpStatus.CONFLICT, 1001, "DUPLICATE_EMAIL", "중복 이메일");
        }

        @GetMapping("/common-status")
        void commonStatus() {
            throw new CommonException(HttpStatus.NOT_FOUND);
        }

        @PostMapping("/valid")
        void valid(@Valid @RequestBody ValidRequest request) {}

        @GetMapping("/constraint")
        void constraint() {
            Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
            Set<ConstraintViolation<ValidRequest>> violations = validator.validate(new ValidRequest(""));
            throw new ConstraintViolationException(violations);
        }

        @GetMapping("/auth")
        void auth() {
            throw new BadCredentialsException("bad credentials");
        }
    }
}
