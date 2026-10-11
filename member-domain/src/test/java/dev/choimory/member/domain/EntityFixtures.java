package dev.choimory.member.domain;

import dev.choimory.member.domain.agreement.command.entity.Agreement;
import dev.choimory.member.domain.agreement.command.entity.AgreementAnswer;
import dev.choimory.member.domain.member.command.entity.Member;
import dev.choimory.member.domain.member.command.type.MemberStatus;
import java.util.UUID;

/**
 * 엔티티 테스트용 객체 생성 도구입니다. unique 제약(email, nickname 등)에 걸리지 않도록 매번 다른 값을 만듭니다.
 */
public final class EntityFixtures {

    private EntityFixtures() {}

    /**
     * 고유한 값을 가진 활성 회원을 생성합니다.
     *
     * @return 생성된 회원
     */
    public static Member member() {
        // 매번 다른 email, nickname을 만든다.
        String unique = UUID.randomUUID().toString();
        return Member.builder()
                .email(unique + "@example.com")
                .password("encoded-password")
                .nickname("nick-" + unique)
                .introduce("introduce")
                .status(MemberStatus.ACTIVATE)
                .build();
    }

    /**
     * 선택 항목(동의, 비동의) 2개를 가진 약관을 생성합니다.
     *
     * @return 생성된 약관
     */
    public static Agreement agreementWithAnswers() {
        // 약관을 만들고 선택 항목 2개를 등록한다.
        Agreement agreement = Agreement.builder()
                .title("이용약관")
                .content("이용약관 내용")
                .isMultiple(false)
                .build();
        agreement.addAnswer(AgreementAnswer.builder()
                .agreement(agreement)
                .name("동의")
                .isPositive(true)
                .build());
        agreement.addAnswer(AgreementAnswer.builder()
                .agreement(agreement)
                .name("비동의")
                .isPositive(false)
                .build());
        return agreement;
    }

    /**
     * 고유한 코드를 만듭니다.
     *
     * @return 고유한 코드 문자열
     */
    public static String uniqueCode() {
        return "CODE-" + UUID.randomUUID();
    }
}
