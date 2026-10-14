package dev.choimory.member.api.authority.v1.command.handler;

import dev.choimory.member.api.authority.v1.command.repository.AuthorityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 권한 명령의 세부 로직을 담당하는 핸들러입니다.
 */
@Component
@RequiredArgsConstructor
public class AuthorityCommandHandler {

    private final AuthorityRepository authorityRepository; // 권한 데이터 접근 객체
}
