package dev.choimory.member.domain.member.command.entity;

import dev.choimory.member.domain.common.command.entity.BaseEntity;
import dev.choimory.member.domain.member.command.type.MemberImageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * 회원 이미지 엔티티입니다. (member_image 테이블)
 * 원본 이미지와 서버가 생성한 리사이즈 이미지의 경로, 파일명, 크기를 함께 가집니다.
 */
@Entity
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MemberImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member; // 회원

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private MemberImageType type; // 이미지 유형

    @Column(name = "path", nullable = false, length = 500)
    private String path; // 원본 이미지 저장 경로

    @Column(name = "name", nullable = false)
    private String name; // 서버에서 변경해 저장한 원본 파일명

    @Column(name = "origin_file_name", nullable = false)
    private String originFileName; // 사용자가 첨부한 파일명

    @Column(name = "size", nullable = false)
    private long size; // 원본 이미지 크기 (byte)

    @Column(name = "resize_file_path", nullable = false, length = 500)
    private String resizeFilePath; // 리사이즈 이미지 저장 경로

    @Column(name = "resize_file_name", nullable = false)
    private String resizeFileName; // 서버가 생성한 리사이즈 파일명

    @Column(name = "resize_file_size", nullable = false)
    private long resizeFileSize; // 리사이즈 이미지 크기 (byte)
}
