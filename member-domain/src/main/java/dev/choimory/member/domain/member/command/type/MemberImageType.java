package dev.choimory.member.domain.member.command.type;

/**
 * 회원 이미지 유형입니다. member_image.type 컬럼에 이름(문자열)으로 저장되므로 이름을 변경하거나 삭제하면 기존 데이터가 깨집니다.
 * 현재는 프로필 이미지만 사용하며 이후 유형이 늘어날 수 있어 유형 컬럼을 유지합니다.
 */
public enum MemberImageType {
    PROFILE // 프로필 이미지
}
