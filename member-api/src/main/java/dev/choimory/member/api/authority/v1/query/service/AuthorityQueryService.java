package dev.choimory.member.api.authority.v1.query.service;

import dev.choimory.member.api.authority.v1.query.handler.AuthorityQueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 권한 조회 API의 비즈니스 로직 흐름을 담당하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class AuthorityQueryService {

    private final AuthorityQueryHandler authorityQueryHandler; // 권한 조회 핸들러
}
