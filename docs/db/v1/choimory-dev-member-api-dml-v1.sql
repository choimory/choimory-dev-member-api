-- ============================================================================
-- choimory-dev-member DML v1 (참고용)
--
--   * 앱과 테스트에서 사용하지 않는다. 실제 적용은 docs/flyway/v1/V3__init_authority_data.sql 이다.
--   * menu 기본 데이터는 메뉴가 구체화될 때 추가한다.
-- ============================================================================

-- authority : 권한
insert into authority (id, name, code, created_at)
values ('01a1538c-4fb2-78c6-893c-f4177b51b5d5', '회원', 'MEMBER', now()),
       ('01a1538c-4fbe-7691-893a-06677cd2fa22', '관리자', 'ADMIN', now())
on conflict (code) do nothing;
