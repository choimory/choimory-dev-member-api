package dev.choimory.member.domain.agreement.command.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import dev.choimory.member.domain.EntityFixtures;
import dev.choimory.member.domain.JpaEntityTest;
import dev.choimory.member.domain.agreement.command.entity.Agreement;
import dev.choimory.member.domain.agreement.command.entity.MemberAgreement;
import dev.choimory.member.domain.member.command.entity.Member;
import jakarta.persistence.EntityManager;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * agreement 패키지 DTO(AgreementDto, AgreementAnswerDto, MemberAgreementDto) 테스트입니다.
 */
@JpaEntityTest
class AgreementDtosTest {

    @Autowired
    private EntityManager em; // 엔티티 매니저

    /**
     * 약관 DTO가 약관의 필드와 소유한 선택 항목을 담는지 확인합니다.
     */
    @Test
    void agreementDtoContainsFieldsAndAnswers() {
        // 선택 항목 2개를 가진 약관을 만든다. (DB를 사용하지 않는다)
        Agreement agreement = EntityFixtures.agreementWithAnswers();

        // DTO로 변환한다.
        AgreementDto dto = AgreementDto.from(agreement);

        // 약관의 필드와 선택 항목이 담겨야 한다.
        assertThat(dto.id()).isEqualTo(agreement.getId());
        assertThat(dto.title()).isEqualTo("이용약관");
        assertThat(dto.content()).isEqualTo("이용약관 내용");
        assertThat(dto.isMultiple()).isFalse();
        assertThat(dto.answers())
                .extracting(AgreementAnswerDto::name, AgreementAnswerDto::isPositive)
                .containsExactlyInAnyOrder(tuple("동의", true), tuple("비동의", false));
    }

    /**
     * 선택 항목 DTO는 상위(약관)를 포함하지 않는지 확인합니다. (순환 참조 방지)
     */
    @Test
    void answerDtoDoesNotContainParent() {
        // 선택 항목 DTO의 필드 이름을 조회한다.
        assertThat(componentNames(AgreementAnswerDto.class)).doesNotContain("agreement", "agreementId");
        assertThat(componentNames(MemberAgreementDto.class)).doesNotContain("member", "memberId");
    }

    /**
     * 회원 동의 이력 DTO가 선택한 항목을 담는지 확인합니다.
     */
    @Test
    void memberAgreementDtoContainsAnswer() {
        // 회원이 선택 항목 하나를 선택한 동의 이력을 만든다.
        Agreement agreement = EntityFixtures.agreementWithAnswers();
        Member member = EntityFixtures.member();
        MemberAgreement memberAgreement = member.addAgreement(MemberAgreement.builder()
                .member(member)
                .agreementAnswer(agreement.getAnswers().get(0))
                .build());

        // DTO로 변환한다.
        MemberAgreementDto dto = MemberAgreementDto.from(memberAgreement);

        // 선택한 항목이 담겨야 한다.
        assertThat(dto.id()).isEqualTo(memberAgreement.getId());
        assertThat(dto.agreementAnswer().id())
                .isEqualTo(agreement.getAnswers().get(0).getId());
        assertThat(dto.agreementAnswer().name()).isEqualTo("동의");
    }

    /**
     * soft delete된 선택 항목은 조회한 약관의 DTO에서 제외되는지 확인합니다.
     */
    @Test
    void softDeletedAnswerIsExcludedFromDto() {
        // 선택 항목 2개를 가진 약관을 저장하고 하나를 soft delete한 뒤 영속성 컨텍스트를 비우고 조회한다.
        Agreement agreement = EntityFixtures.agreementWithAnswers();
        em.persist(agreement);
        agreement.getAnswers().get(0).softDelete();
        em.flush();
        em.clear();
        Agreement loaded = em.find(Agreement.class, agreement.getId());

        // DTO로 변환한다.
        AgreementDto dto = AgreementDto.from(loaded);

        // 삭제되지 않은 선택 항목만 담겨야 한다.
        assertThat(dto.answers()).hasSize(1);
    }

    /**
     * record의 필드 이름 목록을 반환합니다.
     *
     * @param type record 클래스
     * @return 필드 이름 목록
     */
    private List<String> componentNames(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();
    }
}
