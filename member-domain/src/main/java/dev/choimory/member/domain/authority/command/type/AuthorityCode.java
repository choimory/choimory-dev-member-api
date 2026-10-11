package dev.choimory.member.domain.authority.command.type;

/**
 * 권한 코드입니다. authority.code 컬럼에 이름(문자열)으로 저장되므로 이름을 변경하거나 삭제하면 기존 데이터가 깨집니다.
 * 권한은 관리자 페이지에서 동적으로 생성하지 않고 코드 배포로만 추가합니다.
 */
public enum AuthorityCode {
    MEMBER, // 일반 회원
    ADMIN, // 관리자
    MODERATOR // 운영자
}
