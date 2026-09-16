package dev.choimory.member.api.member.v1.query.service;

import dev.choimory.member.api.member.v1.query.domain.document.MemberDocument;
import dev.choimory.member.api.member.v1.query.domain.dto.MemberDocumentDto;
import dev.choimory.member.api.member.v1.query.repository.MemberQueryClient;
import dev.choimory.member.api.member.v1.query.repository.MemberQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 회원 query 세부 로직을 처리하는 핸들러입니다.
 */
@Component
@RequiredArgsConstructor
public class MemberQueryHandler {

    private final PasswordEncoder passwordEncoder; // 비밀번호 암호화 객체
    private final MemberQueryRepository memberQueryRepository; // 회원 query Repository
    private final MemberQueryClient memberQueryClient; // 회원 query Client

    /**
     * 로그인 대상 회원을 조회하고 비밀번호를 검증합니다.
     *
     * @param email 이메일
     * @param password 원문 비밀번호
     * @return 회원 Document DTO
     */
    public MemberDocumentDto loginAndValid(
            String email,
            String password
    ) {
        // 이메일로 회원 Document를 조회한다.
        MemberDocument member = memberQueryRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("아이디가 존재하지 않습니다"));

        // 비밀번호 일치 여부를 확인한다.
        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new BadCredentialsException("비밀번호가 일치하지 않습니다");
        }

        // 조회된 회원 Document를 DTO로 변환한다.
        return MemberDocumentDto.from(member);
    }
}
