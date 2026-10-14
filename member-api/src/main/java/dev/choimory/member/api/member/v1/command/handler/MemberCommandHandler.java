package dev.choimory.member.api.member.v1.command.handler;

import dev.choimory.member.api.member.v1.command.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 회원 명령의 세부 로직을 담당하는 핸들러입니다.
 */
@Component
@RequiredArgsConstructor
public class MemberCommandHandler {

    private final MemberRepository memberRepository; // 회원 데이터 접근 객체
}
