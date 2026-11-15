package dev.choimory.member.api.agreement.v1.query.service;

import dev.choimory.member.api.agreement.v1.query.handler.AgreementQueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 약관 조회 API의 비즈니스 로직 흐름을 담당하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class AgreementQueryService {

    private final AgreementQueryHandler agreementQueryHandler; // 약관 조회 핸들러
}
