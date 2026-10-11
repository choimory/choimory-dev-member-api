package dev.choimory.member.domain.authority.command.type;

/**
 * 메뉴에 대해 허용할 수 있는 액션입니다. permission_action.action 컬럼에 이름(문자열)으로 저장되므로
 * 이름을 변경하거나 삭제하면 기존 데이터가 깨집니다.
 * 엔티티 PermissionAction(permission_action 테이블)과 이름이 겹치지 않도록 Type을 붙였습니다.
 */
public enum PermissionActionType {
    CREATE, // 생성
    READ, // 조회
    UPDATE, // 수정
    DELETE // 삭제
}
