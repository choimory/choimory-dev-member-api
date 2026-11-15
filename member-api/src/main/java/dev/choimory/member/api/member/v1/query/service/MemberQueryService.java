package dev.choimory.member.api.member.v1.query.service;

import dev.choimory.member.api.member.v1.query.handler.MemberQueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 회원 조회 API의 비즈니스 로직 흐름을 담당하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class MemberQueryService {

    private final MemberQueryHandler memberQueryHandler; // 회원 조회 핸들러
}
