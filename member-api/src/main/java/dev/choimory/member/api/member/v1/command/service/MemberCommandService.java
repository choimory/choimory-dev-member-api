package dev.choimory.member.api.member.v1.command.service;

import dev.choimory.member.api.member.v1.command.handler.MemberCommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 회원 명령 API의 비즈니스 로직 흐름을 담당하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class MemberCommandService {

    private final MemberCommandHandler memberCommandHandler; // 회원 명령 핸들러
}
