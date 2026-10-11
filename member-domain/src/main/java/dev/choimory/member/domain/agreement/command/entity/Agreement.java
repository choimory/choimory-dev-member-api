package dev.choimory.member.domain.agreement.command.entity;

import dev.choimory.member.domain.common.command.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * 약관 엔티티입니다. (agreement 테이블)
 * 약관은 수정하지 않고 추가하거나 soft delete만 합니다. 그래야 회원이 동의한 내용을 추적할 수 있습니다.
 * 선택 항목({@link AgreementAnswer})을 소유합니다.
 */
@Entity
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Agreement extends BaseEntity {

    @Column(name = "title", nullable = false)
    private String title; // 약관 제목

    @Column(name = "content", nullable = false)
    private String content; // 약관 내용

    @Column(name = "is_multiple", nullable = false)
    private boolean isMultiple; // 복수 선택 여부 (true는 checkbox, false는 radio)

    @OneToMany(mappedBy = "agreement", cascade = CascadeType.ALL)
    @Builder.Default
    private List<AgreementAnswer> answers = new ArrayList<>(); // 선택 항목

    /**
     * 선택 항목을 등록합니다.
     *
     * @param answer 등록할 선택 항목
     * @return 등록된 answer
     */
    public AgreementAnswer addAnswer(AgreementAnswer answer) {
        this.answers.add(answer);
        return answer;
    }
}
