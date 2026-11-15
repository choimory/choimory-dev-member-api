package dev.choimory.member.api.agreement.v1.command.handler;

import dev.choimory.member.api.agreement.v1.command.repository.AgreementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 약관 명령의 세부 로직을 담당하는 핸들러입니다.
 */
@Component
@RequiredArgsConstructor
public class AgreementCommandHandler {

    private final AgreementRepository agreementRepository; // 약관 데이터 접근 객체
}
