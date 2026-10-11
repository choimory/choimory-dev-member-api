package dev.choimory.member.domain.member.command.type;

/**
 * 회원 상태입니다. member.status 컬럼에 이름(문자열)으로 저장되므로 이름을 변경하거나 삭제하면 기존 데이터가 깨집니다.
 */
public enum MemberStatus {
    REGISTER, // 가입 진행 중
    ACTIVATE, // 활성
    INACTIVATE, // 비활성
    SUSPENDED // 정지
}
