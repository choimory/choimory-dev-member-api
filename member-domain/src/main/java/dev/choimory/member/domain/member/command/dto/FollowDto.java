package dev.choimory.member.domain.member.command.dto;

import dev.choimory.member.domain.member.command.entity.Follow;
import java.time.LocalDateTime;
import lombok.Builder;

/**
 * 팔로우 DTO입니다. Follow 엔티티와 1:1로 매칭됩니다.
 * follower와 followee는 참조만 하는 관계이므로 하위 컬렉션이 null인 회원 DTO로 담습니다.
 *
 * @param id 팔로우 ID
 * @param follower 팔로우하는 회원 (하위 컬렉션 없음)
 * @param followee 팔로우 당하는 회원 (하위 컬렉션 없음)
 * @param createdAt 생성 일시
 * @param modifiedAt 수정 일시
 * @param deletedAt 삭제 일시
 */
@Builder(toBuilder = true)
public record FollowDto(
        String id,
        MemberDto follower,
        MemberDto followee,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        LocalDateTime deletedAt) {

    /**
     * 팔로우 엔티티를 DTO로 변환합니다.
     *
     * @param follow 팔로우 엔티티
     * @return 팔로우 DTO
     */
    public static FollowDto from(Follow follow) {
        return FollowDto.builder()
                .id(follow.getId())
                .follower(MemberDto.fromWithoutChildren(follow.getFollower()))
                .followee(MemberDto.fromWithoutChildren(follow.getFollowee()))
                .createdAt(follow.getCreatedAt())
                .modifiedAt(follow.getModifiedAt())
                .deletedAt(follow.getDeletedAt())
                .build();
    }
}
