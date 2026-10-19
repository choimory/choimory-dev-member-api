package dev.choimory.member.api.member.v1.command.handler;

import dev.choimory.member.api.member.v1.command.exception.MemberDuplicateException;
import dev.choimory.member.api.member.v1.command.payload.request.MemberJoinRequest;
import dev.choimory.member.api.member.v1.command.repository.MemberRepository;
import dev.choimory.member.domain.authority.command.dto.AuthorityDto;
import dev.choimory.member.domain.authority.command.entity.Authority;
import dev.choimory.member.domain.authority.command.entity.MemberAuthority;
import dev.choimory.member.domain.member.command.dto.MemberDto;
import dev.choimory.member.domain.member.command.entity.Member;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 회원 명령의 세부 로직을 담당하는 핸들러입니다.
 */
@Component
@RequiredArgsConstructor
public class MemberCommandHandler {

    private final MemberRepository memberRepository; // 회원 데이터 접근 객체
    private final PasswordEncoder passwordEncoder; // 비밀번호 암호화
    private final EntityManager entityManager; // 다른 도메인의 권한을 조회 없이 참조하기 위한 엔티티 매니저

    /**
     * 회원가입 요청의 이메일과 닉네임 중복을 검증합니다.
     * soft delete된 회원의 값도 hard delete 배치 전까지는 사용할 수 없으므로 삭제된 회원까지 포함해 확인합니다.
     *
     * @param request 회원가입 요청
     * @throws MemberDuplicateException 이메일 또는 닉네임이 이미 사용 중인 경우
     */
    public void validateJoin(MemberJoinRequest request) {
        // 이메일 중복을 확인한다.
        if (memberRepository.existsByEmailIncludingDeleted(request.email())) {
            throw MemberDuplicateException.email();
        }

        // 닉네임 중복을 확인한다.
        if (memberRepository.existsByNicknameIncludingDeleted(request.nickname())) {
            throw MemberDuplicateException.nickname();
        }
    }

    /**
     * 회원을 가입시킵니다. 비밀번호를 암호화하고 부여할 권한과 함께 저장합니다.
     * 동시 요청으로 유니크 제약을 위반하면 저장 시점에 DataIntegrityViolationException이 발생합니다.
     *
     * @param request 회원가입 요청
     * @param authority 부여할 권한
     * @return 저장된 회원 DTO
     */
    public MemberDto join(MemberJoinRequest request, AuthorityDto authority) {
        // 비밀번호를 암호화해 회원을 생성한다.
        Member member = request.toEntity(passwordEncoder.encode(request.password()));

        // 권한은 참조만 하므로 조회 없이 참조만 가져와 회원에 부여한다.
        member.addAuthority(MemberAuthority.builder()
                .member(member)
                .authority(entityManager.getReference(Authority.class, authority.id()))
                .build());

        // 유니크 제약 위반을 이 시점에 확인하도록 즉시 반영한다.
        return MemberDto.from(memberRepository.saveAndFlush(member));
    }
}
