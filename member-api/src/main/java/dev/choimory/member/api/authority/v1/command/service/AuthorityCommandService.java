package dev.choimory.member.api.authority.v1.command.service;

import dev.choimory.member.api.authority.v1.command.handler.AuthorityCommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 권한 명령 API의 비즈니스 로직 흐름을 담당하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class AuthorityCommandService {

    private final AuthorityCommandHandler authorityCommandHandler; // 권한 명령 핸들러
}
