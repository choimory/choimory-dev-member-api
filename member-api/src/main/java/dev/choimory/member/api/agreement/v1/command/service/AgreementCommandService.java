package dev.choimory.member.api.agreement.v1.command.service;

import dev.choimory.member.api.agreement.v1.command.handler.AgreementCommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 약관 명령 API의 비즈니스 로직 흐름을 담당하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class AgreementCommandService {

    private final AgreementCommandHandler agreementCommandHandler; // 약관 명령 핸들러
}
