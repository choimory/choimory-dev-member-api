package dev.choimory.member.api.member.v1.command.service;

import com.github.f4b6a3.uuid.UuidCreator;
import dev.choimory.member.api.common.domain.response.CommonResponse;
import dev.choimory.member.api.member.v1.command.domain.dto.MemberEntityDto;
import dev.choimory.member.api.member.v1.command.domain.request.CreateMemberRequest;
import dev.choimory.member.api.member.v1.command.domain.request.VerifyMemberRequest;
import dev.choimory.member.api.member.v1.command.domain.response.CreateMemberResponse;
import dev.choimory.member.api.member.v1.command.domain.response.VerifyMemberResponse;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 command API 흐름을 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberCommandService {

    private final MemberCommandHandler memberCommandHandler; // 회원 command 핸들러

    /**
     * 회원가입 요청을 처리하고 인증 대기 정보를 저장합니다.
     *
     * @param request 회원가입 요청
     * @return 회원가입 인증 정보 응답
     */
    public CommonResponse<CreateMemberResponse> signUp(CreateMemberRequest request) {
        // 비밀번호를 암호화한다.
        String encodedPassword = memberCommandHandler.encodePassword(request.getPassword());

        // UUID와 인증 코드를 생성한다.
        String uuid = UuidCreator.getTimeOrderedEpoch().toString();
        String verifyCode = memberCommandHandler.generateVerifyCode();

        // Redis에 인증 대기 회원 정보를 저장한다.
        MemberEntityDto member = MemberEntityDto.builder()
                .id(uuid)
                .email(request.getEmail())
                .password(encodedPassword)
                .nickname(request.getNickname())
                .introduce(request.getIntroduce())
                .build();
        memberCommandHandler.setWaitVerifyMember(member, verifyCode, 3, TimeUnit.MINUTES);

        // TODO 이메일 발송 Kafka
        CreateMemberResponse response = CreateMemberResponse.of(uuid, Integer.parseInt(verifyCode));
        return CommonResponse.of(HttpStatus.OK, "SUCCESS", response);
    }

    /**
     * 회원가입 인증 요청을 검증하고 회원 정보를 저장합니다.
     *
     * @param payload 회원 인증 요청
     * @return 회원 인증 완료 응답
     */
    @Transactional
    public CommonResponse<VerifyMemberResponse> verify(VerifyMemberRequest payload) {
        // Redis에서 인증 대기 회원 정보를 조회한다.
        MemberEntityDto member = memberCommandHandler.getWaitVerifyMember(
                payload.getUuid(),
                payload.getEmail(),
                payload.getVerifyCode()
        );
        if (member == null) {
            throw new IllegalArgumentException();
        }

        // 인증 완료 회원 정보를 저장한다.
        MemberEntityDto result = memberCommandHandler.saveVerifiedMember(member);

        // 인증 완료 후 토큰 응답을 생성한다.
        VerifyMemberResponse response = memberCommandHandler.generateToken(result);
        return CommonResponse.of(HttpStatus.CREATED, "SUCCESS", response);
    }
}
