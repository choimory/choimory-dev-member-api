package dev.choimory.member.api.agreement.v1.command.controller;

import dev.choimory.member.api.agreement.v1.command.service.AgreementCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 약관 명령 API 요청을 받는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/v1/agreement")
@RequiredArgsConstructor
public class AgreementCommandController {

    private final AgreementCommandService agreementCommandService; // 약관 명령 서비스
}
