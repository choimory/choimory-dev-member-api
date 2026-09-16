package dev.choimory.member.api.member.v1.command.service;

import dev.choimory.member.api.member.v1.command.domain.dto.MemberEntityDto;
import dev.choimory.member.api.member.v1.command.domain.entity.MemberEntity;
import dev.choimory.member.api.member.v1.command.domain.response.VerifyMemberResponse;
import dev.choimory.member.api.member.v1.command.repository.MemberCommandRepository;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 회원 command 세부 로직을 처리하는 핸들러입니다.
 */
@Component
@RequiredArgsConstructor
public class MemberCommandHandler {

    private final MemberCommandRepository memberCommandRepository; // 회원 command Repository
    private final RedisTemplate<String, Object> redisTemplate; // Redis 접근 객체
    private final PasswordEncoder passwordEncoder; // 비밀번호 암호화 객체

    /**
     * 비밀번호를 암호화합니다.
     *
     * @param password 원문 비밀번호
     * @return 암호화된 비밀번호
     */
    public String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    /**
     * 회원가입 인증 코드를 생성합니다.
     *
     * @return 6자리 인증 코드 문자열
     */
    public String generateVerifyCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }

    /**
     * 인증 대기 회원 정보를 Redis에 저장합니다.
     *
     * @param member 인증 대기 회원 정보
     * @param verifyCode 인증 코드
     * @param ttl 만료 시간
     * @param timeUnit 만료 시간 단위
     */
    public void setWaitVerifyMember(
            MemberEntityDto member,
            String verifyCode,
            long ttl,
            TimeUnit timeUnit
    ) {
        redisTemplate.opsForValue().set(member.getId() + ":" + verifyCode, member, ttl, timeUnit);
    }

    /**
     * 인증 대기 회원 정보를 Redis에서 조회합니다.
     *
     * @param uuid 가입 대기 UUID
     * @param email 이메일
     * @param verifyCode 인증 코드
     * @return 인증 대기 회원 정보
     */
    public MemberEntityDto getWaitVerifyMember(
            String uuid,
            String email,
            int verifyCode
    ) {
        Object member = redisTemplate.opsForValue().get(uuid + ":" + verifyCode);
        if (member instanceof MemberEntityDto memberEntityDto && email.equals(memberEntityDto.getEmail())) {
            return memberEntityDto;
        }
        return null;
    }

    /**
     * 인증 완료 회원 정보를 RDB에 저장합니다.
     *
     * @param member 인증 완료 회원 정보
     * @return 저장된 회원 DTO
     */
    public MemberEntityDto saveVerifiedMember(MemberEntityDto member) {
        MemberEntity memberEntity = MemberEntityDto.toEntity(member);
        MemberEntity result = memberCommandRepository.save(memberEntity);
        return MemberEntityDto.from(result);
    }

    /**
     * 회원 인증 후 응답용 토큰을 생성합니다.
     *
     * @param member 인증 완료 회원 정보
     * @return 토큰 응답
     */
    public VerifyMemberResponse generateToken(MemberEntityDto member) {
        return new VerifyMemberResponse("access", "refresh");
    }
}
