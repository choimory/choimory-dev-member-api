package dev.choimory.member.api.member.v1.command.handler;

import dev.choimory.member.api.common.exception.CommonException;
import dev.choimory.member.api.member.v1.command.payload.request.MemberJoinRequest;
import dev.choimory.member.api.member.v1.command.repository.MemberRepository;
import dev.choimory.member.domain.authority.command.dto.AuthorityDto;
import dev.choimory.member.domain.authority.command.dto.MemberAuthorityDto;
import dev.choimory.member.domain.authority.command.entity.Authority;
import dev.choimory.member.domain.authority.command.entity.MemberAuthority;
import dev.choimory.member.domain.member.command.dto.MemberDto;
import dev.choimory.member.domain.member.command.entity.Member;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 회원 명령의 세부 로직을 담당하는 핸들러입니다.
 */
@Component
@RequiredArgsConstructor
public class MemberCommandHandler {

    private static final String EMAIL_CONSTRAINT = "uk_member_email"; // 이메일 유니크 제약명
    private static final String NICKNAME_CONSTRAINT = "uk_member_nickname"; // 닉네임 유니크 제약명

    private final MemberRepository memberRepository; // 회원 데이터 접근 객체
    private final PasswordEncoder passwordEncoder; // 비밀번호 암호화
    private final EntityManager entityManager; // 다른 도메인의 권한을 조회 없이 참조하기 위한 엔티티 매니저

    /**
     * 회원가입 요청의 이메일과 닉네임 중복을 검증합니다.
     * soft delete된 회원의 값도 hard delete 배치 전까지는 사용할 수 없으므로 삭제된 회원까지 포함해 확인합니다.
     *
     * @param request 회원가입 요청
     * @throws CommonException 이메일 또는 닉네임이 이미 사용 중인 경우(409)
     */
    public void validateJoin(MemberJoinRequest request) {
        // 이메일 중복을 확인한다.
        if (memberRepository.existsByEmailIncludingDeleted(request.email())) {
            throw new CommonException(
                    HttpStatus.CONFLICT, HttpStatus.CONFLICT.value(), "DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다.");
        }

        // 닉네임 중복을 확인한다.
        if (memberRepository.existsByNicknameIncludingDeleted(request.nickname())) {
            throw new CommonException(
                    HttpStatus.CONFLICT, HttpStatus.CONFLICT.value(), "DUPLICATE_NICKNAME", "이미 사용 중인 닉네임입니다.");
        }
    }

    /**
     * 회원을 가입시킵니다. 비밀번호를 암호화하고 부여할 권한과 함께 저장합니다.
     * 사전 검증 이후 동시 요청이 먼저 저장되어 유니크 제약을 위반하면 중복 예외(409)로 변환합니다.
     * 알 수 없는 제약 위반은 변환하지 않고 그대로 던져 서버 오류로 처리합니다.
     *
     * @param request 회원가입 요청
     * @param authority 부여할 권한
     * @return 저장된 회원 DTO
     * @throws CommonException 이메일 또는 닉네임이 이미 사용 중인 경우(409)
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
        try {
            return MemberDto.from(memberRepository.saveAndFlush(member));
        } catch (DataIntegrityViolationException e) {
            // 위반한 제약에 해당하는 중복 예외로 변환하고, 알 수 없는 제약 위반은 그대로 던진다.
            String constraint = violatedConstraint(e);
            if (EMAIL_CONSTRAINT.equals(constraint)) {
                throw new CommonException(
                        HttpStatus.CONFLICT, HttpStatus.CONFLICT.value(), "DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다.");
            }
            if (NICKNAME_CONSTRAINT.equals(constraint)) {
                throw new CommonException(
                        HttpStatus.CONFLICT, HttpStatus.CONFLICT.value(), "DUPLICATE_NICKNAME", "이미 사용 중인 닉네임입니다.");
            }
            throw e;
        }
    }

    /**
     * 이메일로 회원을 조회합니다. 로그인에 필요한 권한만 포함하고 이미지, 정지 이력, 약관은 조회하지 않아 null입니다.
     *
     * @param email 이메일
     * @return 권한을 포함한 회원 DTO, 없거나 삭제된 회원이면 비어 있음
     */
    public Optional<MemberDto> findByEmail(String email) {
        return memberRepository
                .findByEmail(email)
                .map(member -> MemberDto.fromWithoutChildren(member).toBuilder()
                        .authorities(member.getAuthorities().stream()
                                .map(MemberAuthorityDto::from)
                                .toList())
                        .build());
    }

    /**
     * 예외 원인에서 위반한 제약 이름을 찾습니다.
     *
     * @param e 데이터 무결성 위반 예외
     * @return 제약 이름, 찾지 못하면 null
     */
    private String violatedConstraint(DataIntegrityViolationException e) {
        // Hibernate가 제공하는 제약 이름을 우선 사용한다.
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                    && violation.getConstraintName() != null) {
                return violation.getConstraintName();
            }
        }

        // 제약 이름을 얻지 못하면 DB 오류 메시지에서 알려진 제약 이름을 찾는다.
        String message = e.getMostSpecificCause().getMessage();
        if (message != null) {
            for (String constraint : List.of(EMAIL_CONSTRAINT, NICKNAME_CONSTRAINT)) {
                if (message.contains(constraint)) {
                    return constraint;
                }
            }
        }
        return null;
    }
}
