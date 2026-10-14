-- ============================================================================
-- V2 : timestamp -> timestamptz
--
--   * 모든 시각 컬럼(created_at, modified_at, deleted_at, suspended_at, suspended_to)을
--     timestamp(시간대 없음)에서 timestamptz(UTC로 정규화되어 저장)로 바꾼다.
--   * 기존 timestamp 값은 앱이 서버 시간대(Asia/Seoul)로 쓴 값이므로
--     using ... at time zone 'Asia/Seoul' 로 올바른 시각으로 변환한다.
--     (시간대를 명시하지 않으면 실행 시점의 세션 시간대로 해석되어 DB 환경마다 결과가 달라진다)
--   * 이미 적용된 V1은 수정하지 않고 변경을 새 버전으로 추가한다. (Flyway는 적용된 파일의 체크섬을 검증한다)
--   * 타입 변경은 COMMENT에 영향이 없으므로 COMMENT 문은 다시 쓰지 않는다.
-- ============================================================================

alter table member
    alter column created_at type timestamptz using created_at at time zone 'Asia/Seoul',
    alter column modified_at type timestamptz using modified_at at time zone 'Asia/Seoul',
    alter column deleted_at type timestamptz using deleted_at at time zone 'Asia/Seoul';

alter table member_image
    alter column created_at type timestamptz using created_at at time zone 'Asia/Seoul',
    alter column modified_at type timestamptz using modified_at at time zone 'Asia/Seoul',
    alter column deleted_at type timestamptz using deleted_at at time zone 'Asia/Seoul';

alter table member_suspension
    alter column suspended_at type timestamptz using suspended_at at time zone 'Asia/Seoul',
    alter column suspended_to type timestamptz using suspended_to at time zone 'Asia/Seoul',
    alter column created_at type timestamptz using created_at at time zone 'Asia/Seoul',
    alter column modified_at type timestamptz using modified_at at time zone 'Asia/Seoul',
    alter column deleted_at type timestamptz using deleted_at at time zone 'Asia/Seoul';

alter table follow
    alter column created_at type timestamptz using created_at at time zone 'Asia/Seoul',
    alter column modified_at type timestamptz using modified_at at time zone 'Asia/Seoul',
    alter column deleted_at type timestamptz using deleted_at at time zone 'Asia/Seoul';

alter table authority
    alter column created_at type timestamptz using created_at at time zone 'Asia/Seoul',
    alter column modified_at type timestamptz using modified_at at time zone 'Asia/Seoul',
    alter column deleted_at type timestamptz using deleted_at at time zone 'Asia/Seoul';

alter table menu
    alter column created_at type timestamptz using created_at at time zone 'Asia/Seoul',
    alter column modified_at type timestamptz using modified_at at time zone 'Asia/Seoul',
    alter column deleted_at type timestamptz using deleted_at at time zone 'Asia/Seoul';

alter table member_authority
    alter column created_at type timestamptz using created_at at time zone 'Asia/Seoul',
    alter column modified_at type timestamptz using modified_at at time zone 'Asia/Seoul',
    alter column deleted_at type timestamptz using deleted_at at time zone 'Asia/Seoul';

alter table permission
    alter column created_at type timestamptz using created_at at time zone 'Asia/Seoul',
    alter column modified_at type timestamptz using modified_at at time zone 'Asia/Seoul',
    alter column deleted_at type timestamptz using deleted_at at time zone 'Asia/Seoul';

alter table permission_action
    alter column created_at type timestamptz using created_at at time zone 'Asia/Seoul',
    alter column modified_at type timestamptz using modified_at at time zone 'Asia/Seoul',
    alter column deleted_at type timestamptz using deleted_at at time zone 'Asia/Seoul';

alter table agreement
    alter column created_at type timestamptz using created_at at time zone 'Asia/Seoul',
    alter column modified_at type timestamptz using modified_at at time zone 'Asia/Seoul',
    alter column deleted_at type timestamptz using deleted_at at time zone 'Asia/Seoul';

alter table agreement_answer
    alter column created_at type timestamptz using created_at at time zone 'Asia/Seoul',
    alter column modified_at type timestamptz using modified_at at time zone 'Asia/Seoul',
    alter column deleted_at type timestamptz using deleted_at at time zone 'Asia/Seoul';

alter table member_agreement
    alter column created_at type timestamptz using created_at at time zone 'Asia/Seoul',
    alter column modified_at type timestamptz using modified_at at time zone 'Asia/Seoul',
    alter column deleted_at type timestamptz using deleted_at at time zone 'Asia/Seoul';
