package dev.choimory.member.domain.agreement.command.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import dev.choimory.member.domain.EntityFixtures;
import dev.choimory.member.domain.JpaEntityTest;
import dev.choimory.member.domain.member.command.entity.Member;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * agreement 패키지 엔티티(Agreement, AgreementAnswer, MemberAgreement) 테스트입니다.
 */
@JpaEntityTest
class AgreementEntitiesTest {

    @Autowired
    private EntityManager em; // 엔티티 매니저

    @Autowired
    private JdbcTemplate jdbc; // JDBC 템플릿

    // ---------------------------------------------------------------------------------------------
    // Agreement, AgreementAnswer
    // ---------------------------------------------------------------------------------------------

    /**
     * 약관 생성 시 필드가 설정되고 선택 항목이 비어 있는지 확인합니다.
     */
    @Test
    void createSetsFields() {
        // 복수 선택 약관을 생성한다.
        Agreement agreement = Agreement.builder()
                .title("마케팅")
                .content("마케팅 내용")
                .isMultiple(true)
                .build();

        // 전달한 값이 설정되고 선택 항목은 비어 있어야 한다.
        assertThat(agreement.getTitle()).isEqualTo("마케팅");
        assertThat(agreement.getContent()).isEqualTo("마케팅 내용");
        assertThat(agreement.isMultiple()).isTrue();
        assertThat(agreement.getAnswers()).isEmpty();
    }

    /**
     * 선택 항목이 약관에 등록되고 약관과 함께 저장되며 조회 시 값이 같은지 확인합니다.
     */
    @Test
    void answersAreRegisteredAndCascaded() {
        // 선택 항목 2개를 가진 약관을 만든다.
        Agreement agreement = EntityFixtures.agreementWithAnswers();
        assertThat(agreement.getAnswers()).hasSize(2);
        assertThat(agreement.getAnswers().get(0).getAgreement()).isSameAs(agreement);

        // 약관만 저장하고 영속성 컨텍스트를 비운 뒤 조회한다.
        em.persist(agreement);
        em.flush();
        em.clear();
        Agreement loaded = em.find(Agreement.class, agreement.getId());

        // 선택 항목이 함께 저장되어 조회되어야 한다.
        assertThat(loaded.isMultiple()).isFalse();
        assertThat(loaded.getAnswers())
                .extracting(AgreementAnswer::getName, AgreementAnswer::isPositive)
                .containsExactlyInAnyOrder(tuple("동의", true), tuple("비동의", false));
    }

    /**
     * soft delete된 선택 항목은 약관의 선택 항목 목록에서 제외되는지 확인합니다.
     */
    @Test
    void softDeletedAnswerIsExcludedFromCollection() {
        // 선택 항목 2개를 가진 약관을 저장하고 하나를 soft delete한다.
        Agreement agreement = EntityFixtures.agreementWithAnswers();
        em.persist(agreement);
        agreement.getAnswers().get(0).softDelete();
        em.flush();
        em.clear();

        // 삭제되지 않은 선택 항목만 조회되어야 한다.
        Agreement loaded = em.find(Agreement.class, agreement.getId());
        assertThat(loaded.getAnswers()).hasSize(1);
    }

    // ---------------------------------------------------------------------------------------------
    // MemberAgreement
    // ---------------------------------------------------------------------------------------------

    /**
     * 회원의 동의 이력이 회원과 함께 저장되고, 회원을 삭제해도 약관 선택 항목은 남는지 확인합니다.
     */
    @Test
    void memberAgreementIsCascadedFromMemberAndDoesNotRemoveAnswer() {
        // 약관은 따로 저장하고 회원이 선택 항목 하나를 선택한다.
        Agreement agreement = EntityFixtures.agreementWithAnswers();
        em.persist(agreement);
        AgreementAnswer answer = agreement.getAnswers().get(0);
        Member member = EntityFixtures.member();
        MemberAgreement memberAgreement = member.addAgreement(
                MemberAgreement.builder().member(member).agreementAnswer(answer).build());
        assertThat(member.getAgreements()).containsExactly(memberAgreement);

        // 회원만 저장하고 영속성 컨텍스트를 비운 뒤 조회한다.
        em.persist(member);
        em.flush();
        em.clear();
        Member loaded = em.find(Member.class, member.getId());

        // 동의 이력이 함께 저장되어 조회되고 선택한 항목이 같아야 한다.
        assertThat(loaded.getAgreements()).hasSize(1);
        assertThat(loaded.getAgreements().get(0).getAgreementAnswer().getId()).isEqualTo(answer.getId());

        // 회원을 삭제하면 동의 이력은 삭제되고 약관 선택 항목은 남아 있어야 한다.
        em.remove(loaded);
        em.flush();
        assertThat(count("member_agreement")).isZero();
        assertThat(count("agreement_answer")).isEqualTo(2);
    }

    /**
     * 같은 회원이 같은 선택 항목을 중복 선택할 수 없는지 확인합니다.
     */
    @Test
    void duplicateMemberAgreementIsRejected() {
        // 약관을 저장하고 같은 회원이 같은 선택 항목을 두 번 선택한다.
        Agreement agreement = EntityFixtures.agreementWithAnswers();
        em.persist(agreement);
        AgreementAnswer answer = agreement.getAnswers().get(0);
        Member member = EntityFixtures.member();
        member.addAgreement(
                MemberAgreement.builder().member(member).agreementAnswer(answer).build());
        member.addAgreement(
                MemberAgreement.builder().member(member).agreementAnswer(answer).build());
        em.persist(member);

        // unique 제약 위반이어야 한다.
        assertThatThrownBy(() -> em.flush()).isInstanceOf(PersistenceException.class);
    }

    /**
     * 테이블의 행 개수를 조회합니다.
     *
     * @param table 테이블 이름
     * @return 행 개수
     */
    private int count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Integer.class);
    }
}
