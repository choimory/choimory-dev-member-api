package dev.choimory.member.api.member.v1.query.service;

import dev.choimory.member.api.member.v1.query.domain.dto.MemberDocumentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 회원 query API 흐름을 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class MemberQueryService {

    private final MemberQueryHandler memberQueryHandler; // 회원 query 핸들러

    /**
     * 로그인 대상 회원을 검증합니다.
     *
     * @param email 이메일
     * @param password 원문 비밀번호
     * @return 회원 Document DTO
     */
    public MemberDocumentDto login(String email, String password) {
        // 로그인 대상 회원을 검증하고 조회한다.
        return memberQueryHandler.loginAndValid(email, password);
    }
}
