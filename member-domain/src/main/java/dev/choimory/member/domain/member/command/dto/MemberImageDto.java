package dev.choimory.member.domain.member.command.dto;

import dev.choimory.member.domain.member.command.entity.MemberImage;
import dev.choimory.member.domain.member.command.type.MemberImageType;
import java.time.Instant;
import lombok.Builder;

/**
 * 회원 이미지 DTO입니다. MemberImage 엔티티와 1:1로 매칭되며 상위(회원)를 포함하지 않습니다.
 *
 * @param id 회원 이미지 ID
 * @param type 이미지 유형
 * @param path 원본 이미지 저장 경로
 * @param name 서버에서 변경해 저장한 원본 파일명
 * @param originFileName 사용자가 첨부한 파일명
 * @param size 원본 이미지 크기 (byte)
 * @param resizeFilePath 리사이즈 이미지 저장 경로
 * @param resizeFileName 서버가 생성한 리사이즈 파일명
 * @param resizeFileSize 리사이즈 이미지 크기 (byte)
 * @param createdAt 생성 일시
 * @param modifiedAt 수정 일시
 * @param deletedAt 삭제 일시
 */
@Builder(toBuilder = true)
public record MemberImageDto(
        String id,
        MemberImageType type,
        String path,
        String name,
        String originFileName,
        long size,
        String resizeFilePath,
        String resizeFileName,
        long resizeFileSize,
        Instant createdAt,
        Instant modifiedAt,
        Instant deletedAt) {

    /**
     * 회원 이미지 엔티티를 DTO로 변환합니다.
     *
     * @param image 회원 이미지 엔티티
     * @return 회원 이미지 DTO
     */
    public static MemberImageDto from(MemberImage image) {
        return MemberImageDto.builder()
                .id(image.getId())
                .type(image.getType())
                .path(image.getPath())
                .name(image.getName())
                .originFileName(image.getOriginFileName())
                .size(image.getSize())
                .resizeFilePath(image.getResizeFilePath())
                .resizeFileName(image.getResizeFileName())
                .resizeFileSize(image.getResizeFileSize())
                .createdAt(image.getCreatedAt())
                .modifiedAt(image.getModifiedAt())
                .deletedAt(image.getDeletedAt())
                .build();
    }
}
