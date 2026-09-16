package dev.choimory.member.api.common.domain.document;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * Elasticsearch 문서의 공통 일시 정보를 표현하는 객체입니다.
 */
@Getter
@Builder(toBuilder = true)
public final class CommonDateDocument {

    private final LocalDateTime createdAt; // 생성 일시
    private final LocalDateTime modifiedAt; // 수정 일시
    private final LocalDateTime deletedAt; // 삭제 일시

    /**
     * 공통 일시 문서 객체를 생성합니다.
     *
     * @param createdAt 생성 일시
     * @param modifiedAt 수정 일시
     * @param deletedAt 삭제 일시
     */
    @JsonCreator
    public CommonDateDocument(
            @JsonProperty("createdAt") LocalDateTime createdAt,
            @JsonProperty("modifiedAt") LocalDateTime modifiedAt,
            @JsonProperty("deletedAt") LocalDateTime deletedAt
    ) {
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
        this.deletedAt = deletedAt;
    }
}
