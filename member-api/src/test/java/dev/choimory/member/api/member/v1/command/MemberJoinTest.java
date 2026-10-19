package dev.choimory.member.api.member.v1.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.choimory.member.api.TestcontainersConfiguration;
import dev.choimory.member.api.authority.v1.command.handler.AuthorityCommandHandler;
import dev.choimory.member.api.common.exception.CommonException;
import dev.choimory.member.api.member.v1.command.handler.MemberCommandHandler;
import dev.choimory.member.api.member.v1.command.payload.request.MemberJoinRequest;
import dev.choimory.member.api.member.v1.command.repository.MemberRepository;
import dev.choimory.member.domain.authority.command.dto.AuthorityDto;
import dev.choimory.member.domain.authority.command.type.AuthorityCode;
import dev.choimory.member.domain.member.command.entity.Member;
import dev.choimory.member.domain.member.command.type.MemberStatus;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 회원가입 API 통합 테스트입니다. 실제 PostgreSQL(Testcontainers)과 Flyway 스키마를 사용합니다.
 * Security 설정은 이후 단계에서 작성하므로 이 테스트에서는 Security 필터를 끄고 가입 흐름만 검증합니다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("local")
class MemberJoinTest {

    private static final String URL = "/v1/member/join"; // 회원가입 주소

    @Autowired
    private MockMvc mockMvc; // MockMvc

    @Autowired
    private ObjectMapper objectMapper; // JSON 변환

    @Autowired
    private JdbcTemplate jdbc; // JDBC 템플릿

    @Autowired
    private MemberRepository memberRepository; // 회원 데이터 접근 객체

    @Autowired
    private PasswordEncoder passwordEncoder; // 비밀번호 인코더

    @Autowired
    private MemberCommandHandler memberCommandHandler; // 회원 명령 핸들러

    @Autowired
    private AuthorityCommandHandler authorityCommandHandler; // 권한 명령 핸들러

    @Autowired
    private TransactionTemplate transactionTemplate; // 서비스의 @Transactional을 대신하는 트랜잭션 템플릿

    /**
     * 회원 데이터를 정리합니다. 권한 기본 데이터(V3)는 마이그레이션으로 들어 있으므로 남겨 둡니다.
     */
    @BeforeEach
    @AfterEach
    void cleanUp() {
        jdbc.execute("truncate table member_authority, member cascade");
    }

    /**
     * 정상 가입 시 201 응답과 함께 회원이 ACTIVATE 상태로 저장되고 MEMBER 권한이 부여됩니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void joinSavesMemberWithMemberAuthority() throws Exception {
        // 정상 요청으로 가입한다.
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json(request("a@a.com", "nick1"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value("a@a.com"))
                .andExpect(jsonPath("$.data.nickname").value("nick1"))
                .andExpect(jsonPath("$.data.status").value("ACTIVATE"))
                .andExpect(jsonPath("$.data.password").doesNotExist());

        // 회원은 ACTIVATE 상태로 저장되고 비밀번호는 암호화되어 있어야 한다.
        Member member = memberRepository.findAll().get(0);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVATE);
        assertThat(member.getPassword()).isNotEqualTo("Password1!");
        assertThat(passwordEncoder.matches("Password1!", member.getPassword())).isTrue();

        // MEMBER 권한이 부여되어 있어야 한다.
        List<String> codes = jdbc.queryForList(
                "select a.code from member_authority ma join authority a on a.id = ma.authority_id", String.class);
        assertThat(codes).containsExactly("MEMBER");
    }

    /**
     * 이미 사용 중인 이메일로 가입하면 409 응답이어야 합니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void duplicateEmailIsConflict() throws Exception {
        // 먼저 가입한다.
        join(request("a@a.com", "nick1")).andExpect(status().isCreated());

        // 같은 이메일, 다른 닉네임으로 가입하면 이메일 중복이어야 한다.
        join(request("a@a.com", "nick2"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.name").value("DUPLICATE_EMAIL"));
    }

    /**
     * 이미 사용 중인 닉네임으로 가입하면 409 응답이어야 합니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void duplicateNicknameIsConflict() throws Exception {
        // 먼저 가입한다.
        join(request("a@a.com", "nick1")).andExpect(status().isCreated());

        // 다른 이메일, 같은 닉네임으로 가입하면 닉네임 중복이어야 한다.
        join(request("b@b.com", "nick1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.name").value("DUPLICATE_NICKNAME"));
    }

    /**
     * soft delete된 회원의 이메일과 닉네임은 hard delete 전까지 사용할 수 없습니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void softDeletedEmailAndNicknameCannotBeReused() throws Exception {
        // 가입 후 회원을 soft delete 한다.
        join(request("a@a.com", "nick1")).andExpect(status().isCreated());
        jdbc.update("update member set deleted_at = now()");
        assertThat(memberRepository.findAll()).isEmpty();

        // 삭제된 회원의 이메일로 가입하면 409여야 한다.
        join(request("a@a.com", "nick2"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.name").value("DUPLICATE_EMAIL"));

        // 삭제된 회원의 닉네임으로 가입하면 409여야 한다.
        join(request("b@b.com", "nick1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.name").value("DUPLICATE_NICKNAME"));
    }

    /**
     * 요청 값 검증에 실패하면 400 응답과 실패 필드 목록이어야 합니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void invalidRequestIsBadRequest() throws Exception {
        // 형식이 잘못된 이메일, 약한 비밀번호, 짧은 닉네임으로 가입한다.
        join(MemberJoinRequest.builder()
                        .email("not-email")
                        .password("weak")
                        .nickname("a")
                        .build())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[*].field").value(hasItems("email", "password", "nickname")));
        assertThat(memberRepository.findAll()).isEmpty();
    }

    /**
     * 도메인(점)이 없는 이메일은 거부됩니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void emailWithoutDomainDotIsBadRequest() throws Exception {
        // a@b 형식으로 가입하면 이메일 검증에 실패해야 한다.
        join(request("a@b", "nick1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[*].field").value(hasItems("email")));
    }

    /**
     * 닉네임은 특수문자를 제외한 한글, 영문, 숫자만 3~15자까지 허용합니다.
     *
     * @throws Exception MockMvc 수행 중 발생
     */
    @Test
    void nicknameAllowsOnlyKoreanEnglishDigitsWithLength3To15() throws Exception {
        // 경계 길이(3자, 15자)와 한글, 영문, 숫자 조합은 가입되어야 한다.
        join(request("a1@a.com", "abc")).andExpect(status().isCreated());
        join(request("a2@a.com", "가나다라마바사아자차카타파하1")).andExpect(status().isCreated());
        join(request("a3@a.com", "닉네임Abc123")).andExpect(status().isCreated());

        // 2자, 16자, 특수문자, 공백이 들어간 닉네임은 거부되어야 한다.
        for (String nickname : List.of("ab", "abcdefghijklmnop", "nick_1", "nick!", "nick 1")) {
            join(request("b@b.com", nickname))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data[*].field").value(hasItems("nickname")));
        }
    }

    /**
     * 사전 검증을 통과한 동시 요청이 유니크 제약에 걸리면 제약 이름에 맞는 409 공통 예외로 변환됩니다.
     */
    @Test
    void uniqueViolationIsConvertedToConflict() {
        // 사전 검증을 건너뛰고 저장하는 상황(동시 요청)을 만들기 위해 서비스의 트랜잭션처럼 감싸 핸들러의 join을 직접 호출한다.
        AuthorityDto authority = authorityCommandHandler.findByCode(AuthorityCode.MEMBER);
        inTransaction(() -> memberCommandHandler.join(request("a@a.com", "nick1"), authority));

        // 이메일이 겹치면 이메일 중복 409여야 한다.
        assertThatThrownBy(() -> inTransaction(() -> memberCommandHandler.join(request("a@a.com", "nick2"), authority)))
                .isInstanceOfSatisfying(CommonException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCodeName()).isEqualTo("DUPLICATE_EMAIL");
                });

        // 닉네임이 겹치면 닉네임 중복 409여야 한다.
        assertThatThrownBy(() -> inTransaction(() -> memberCommandHandler.join(request("b@b.com", "nick1"), authority)))
                .isInstanceOfSatisfying(CommonException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCodeName()).isEqualTo("DUPLICATE_NICKNAME");
                });
    }

    /**
     * 회원가입 요청을 수행합니다.
     *
     * @param request 회원가입 요청
     * @return 수행 결과
     * @throws Exception MockMvc 수행 중 발생
     */
    private org.springframework.test.web.servlet.ResultActions join(MemberJoinRequest request) throws Exception {
        return mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json(request)));
    }

    /**
     * 요청 객체를 JSON 문자열로 변환합니다.
     *
     * @param request 회원가입 요청
     * @return JSON 문자열
     * @throws Exception 변환 중 발생
     */
    private String json(MemberJoinRequest request) throws Exception {
        return objectMapper.writeValueAsString(request);
    }

    /**
     * 정상 회원가입 요청을 생성합니다.
     *
     * @param email 이메일
     * @param nickname 닉네임
     * @return 회원가입 요청
     */
    private MemberJoinRequest request(String email, String nickname) {
        return MemberJoinRequest.builder()
                .email(email)
                .password("Password1!")
                .nickname(nickname)
                .build();
    }

    /**
     * 서비스의 @Transactional처럼 트랜잭션 안에서 작업을 수행합니다.
     *
     * @param action 수행할 작업
     */
    private void inTransaction(Runnable action) {
        transactionTemplate.executeWithoutResult(status -> action.run());
    }
}
