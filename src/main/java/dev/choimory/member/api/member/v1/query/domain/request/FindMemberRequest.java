package dev.choimory.member.api.member.v1.query.domain.request;

import lombok.Builder;

/**
 * 회원 조회 요청 객체입니다.
 */
@Builder(toBuilder = true)
public record FindMemberRequest() {
}
