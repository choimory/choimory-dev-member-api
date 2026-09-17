package dev.choimory.member.api.common.domain.document;

import java.time.LocalDateTime;
import lombok.Builder;

/**
 * Elasticsearch 문서의 공통 일시 정보를 표현하는 객체입니다.
 */
@Builder(toBuilder = true)
public record CommonDateDocument(
        LocalDateTime createdAt, // 생성 일시
        LocalDateTime modifiedAt, // 수정 일시
        LocalDateTime deletedAt // 삭제 일시
        ) {}
