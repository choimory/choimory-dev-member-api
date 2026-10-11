-- ============================================================================
-- choimory-dev-member DDL v1
--
-- 기준: .agents/histories/2026/02/17/modeling/plan1.md 의 DB2, plan4.md 의 최종 구현 계획
--
-- 공통 규칙
--   * id 는 varchar(36), 서버(비즈니스 로직)에서 UUID v7 로 채번한다. 외래키 컬럼도 varchar(36) 이다.
--   * 모든 테이블은 공통 컬럼 id, created_at, modified_at, deleted_at 을 가진다.
--   * 삭제는 deleted_at 을 이용한 soft delete 가 기본이다.
--     follow 는 비즈니스 로직에서 hard delete 하고, member 는 탈퇴 후 배치로 hard delete 한다.
--   * enum 컬럼은 DB enum 이 아닌 varchar 이며 허용 값은 서버 enum 에서만 관리한다.
--   * unique 제약은 soft delete 된 행까지 포함한다.
--   * 외래키 삭제 동작
--       소유 관계(부모가 자식을 소유)는 ON DELETE CASCADE
--       참조 관계(다른 엔티티를 참조만 함)는 ON DELETE RESTRICT
--   * 이 파일은 local 프로필에서 앱이 뜰 때마다 실행되므로 여러 번 실행해도 안전해야 한다.
--     DROP 문은 작성하지 않는다.
-- ============================================================================


-- ----------------------------------------------------------------------------
-- member : 회원
-- ----------------------------------------------------------------------------
create table if not exists member
(
    id          varchar(36)  not null,
    email       varchar(255) not null,
    password    varchar(255) not null,
    nickname    varchar(255) not null,
    introduce   text,
    status      varchar(20)  not null, -- REGISTER, ACTIVATE, INACTIVATE, SUSPENDED
    created_at  timestamp    not null,
    modified_at timestamp,
    deleted_at  timestamp,
    constraint pk_member primary key (id),
    constraint uk_member_email unique (email),
    constraint uk_member_nickname unique (nickname)
);

-- ----------------------------------------------------------------------------
-- member_image : 회원 이미지 (member 1:N)
-- ----------------------------------------------------------------------------
create table if not exists member_image
(
    id               varchar(36)  not null,
    member_id        varchar(36)  not null,
    type             varchar(20)  not null, -- PROFILE
    path             varchar(500) not null,
    name             varchar(255) not null,
    origin_file_name varchar(255) not null,
    size             bigint       not null,
    resize_file_path varchar(500) not null,
    resize_file_name varchar(255) not null,
    resize_file_size bigint       not null,
    created_at       timestamp    not null,
    modified_at      timestamp,
    deleted_at       timestamp,
    constraint pk_member_image primary key (id),
    constraint fk_member_image_member foreign key (member_id) references member (id) on delete cascade
);

create index if not exists idx_member_image_member_id on member_image (member_id);

-- ----------------------------------------------------------------------------
-- member_suspension : 회원 정지 이력 (member 1:N)
-- ----------------------------------------------------------------------------
create table if not exists member_suspension
(
    id            varchar(36)  not null,
    member_id     varchar(36)  not null,
    suspended_at  timestamp    not null,
    suspended_to  timestamp    not null,
    reason        varchar(500) not null,
    created_at    timestamp    not null,
    modified_at   timestamp,
    deleted_at    timestamp,
    constraint pk_member_suspension primary key (id),
    constraint fk_member_suspension_member foreign key (member_id) references member (id) on delete cascade
);

create index if not exists idx_member_suspension_member_id on member_suspension (member_id);

-- ----------------------------------------------------------------------------
-- follow : 팔로우 (member N:N member), hard delete
-- ----------------------------------------------------------------------------
create table if not exists follow
(
    id          varchar(36) not null,
    follower_id varchar(36) not null,
    followee_id varchar(36) not null,
    created_at  timestamp   not null,
    modified_at timestamp,
    deleted_at  timestamp,
    constraint pk_follow primary key (id),
    constraint uk_follow_follower_followee unique (follower_id, followee_id),
    constraint ck_follow_not_self check (follower_id <> followee_id),
    constraint fk_follow_follower foreign key (follower_id) references member (id) on delete cascade,
    constraint fk_follow_followee foreign key (followee_id) references member (id) on delete cascade
);

create index if not exists idx_follow_followee_id on follow (followee_id);

-- ----------------------------------------------------------------------------
-- authority : 권한
-- ----------------------------------------------------------------------------
create table if not exists authority
(
    id          varchar(36)  not null,
    name        varchar(100) not null,
    code        varchar(50)  not null, -- MEMBER, ADMIN, MODERATOR
    created_at  timestamp    not null,
    modified_at timestamp,
    deleted_at  timestamp,
    constraint pk_authority primary key (id),
    constraint uk_authority_code unique (code)
);

-- ----------------------------------------------------------------------------
-- menu : 메뉴 (자기참조 부모 메뉴, 하위 메뉴가 있으면 삭제할 수 없다)
-- ----------------------------------------------------------------------------
create table if not exists menu
(
    id             varchar(36)  not null,
    parent_menu_id varchar(36),
    name           varchar(100) not null,
    code           varchar(100) not null,
    path           varchar(255) not null,
    created_at     timestamp    not null,
    modified_at    timestamp,
    deleted_at     timestamp,
    constraint pk_menu primary key (id),
    constraint uk_menu_code unique (code),
    constraint fk_menu_parent foreign key (parent_menu_id) references menu (id) on delete restrict
);

create index if not exists idx_menu_parent_menu_id on menu (parent_menu_id);

-- ----------------------------------------------------------------------------
-- member_authority : 회원 권한 (member N:N authority)
-- ----------------------------------------------------------------------------
create table if not exists member_authority
(
    id           varchar(36) not null,
    member_id    varchar(36) not null,
    authority_id varchar(36) not null,
    created_at   timestamp   not null,
    modified_at  timestamp,
    deleted_at   timestamp,
    constraint pk_member_authority primary key (id),
    constraint uk_member_authority_member_authority unique (member_id, authority_id),
    constraint fk_member_authority_member foreign key (member_id) references member (id) on delete cascade,
    constraint fk_member_authority_authority foreign key (authority_id) references authority (id) on delete restrict
);

create index if not exists idx_member_authority_authority_id on member_authority (authority_id);

-- ----------------------------------------------------------------------------
-- permission : 권한별 메뉴 접근 (authority N:N menu)
-- ----------------------------------------------------------------------------
create table if not exists permission
(
    id           varchar(36) not null,
    authority_id varchar(36) not null,
    menu_id      varchar(36) not null,
    created_at   timestamp   not null,
    modified_at  timestamp,
    deleted_at   timestamp,
    constraint pk_permission primary key (id),
    constraint uk_permission_authority_menu unique (authority_id, menu_id),
    constraint fk_permission_authority foreign key (authority_id) references authority (id) on delete cascade,
    constraint fk_permission_menu foreign key (menu_id) references menu (id) on delete restrict
);

create index if not exists idx_permission_menu_id on permission (menu_id);

-- ----------------------------------------------------------------------------
-- permission_action : permission 에 허용된 액션 (permission 1:N)
-- ----------------------------------------------------------------------------
create table if not exists permission_action
(
    id            varchar(36) not null,
    permission_id varchar(36) not null,
    action        varchar(20) not null, -- CREATE, READ, UPDATE, DELETE
    created_at    timestamp   not null,
    modified_at   timestamp,
    deleted_at    timestamp,
    constraint pk_permission_action primary key (id),
    constraint uk_permission_action_permission_action unique (permission_id, action),
    constraint fk_permission_action_permission foreign key (permission_id) references permission (id) on delete cascade
);

-- ----------------------------------------------------------------------------
-- agreement : 약관 (수정하지 않고 추가 또는 soft delete 만 한다)
-- ----------------------------------------------------------------------------
create table if not exists agreement
(
    id          varchar(36)  not null,
    title       varchar(255) not null,
    content     text         not null,
    is_multiple boolean      not null,
    created_at  timestamp    not null,
    modified_at timestamp,
    deleted_at  timestamp,
    constraint pk_agreement primary key (id)
);

-- ----------------------------------------------------------------------------
-- agreement_answer : 약관의 선택 항목 (agreement 1:N)
-- ----------------------------------------------------------------------------
create table if not exists agreement_answer
(
    id           varchar(36)  not null,
    agreement_id varchar(36)  not null,
    name         varchar(255) not null,
    is_positive  boolean      not null,
    created_at   timestamp    not null,
    modified_at  timestamp,
    deleted_at   timestamp,
    constraint pk_agreement_answer primary key (id),
    constraint fk_agreement_answer_agreement foreign key (agreement_id) references agreement (id) on delete cascade
);

create index if not exists idx_agreement_answer_agreement_id on agreement_answer (agreement_id);

-- ----------------------------------------------------------------------------
-- member_agreement : 회원이 선택한 약관 항목 (member N:N agreement_answer)
-- 동의 이력 보존을 위해 선택된 항목(agreement_answer)은 삭제할 수 없다.
-- ----------------------------------------------------------------------------
create table if not exists member_agreement
(
    id                  varchar(36) not null,
    member_id           varchar(36) not null,
    agreement_answer_id varchar(36) not null,
    created_at          timestamp   not null,
    modified_at         timestamp,
    deleted_at          timestamp,
    constraint pk_member_agreement primary key (id),
    constraint uk_member_agreement_member_answer unique (member_id, agreement_answer_id),
    constraint fk_member_agreement_member foreign key (member_id) references member (id) on delete cascade,
    constraint fk_member_agreement_answer foreign key (agreement_answer_id) references agreement_answer (id) on delete restrict
);

create index if not exists idx_member_agreement_agreement_answer_id on member_agreement (agreement_answer_id);


-- ============================================================================
-- COMMENT (COMMENT 문은 여러 번 실행해도 안전하다)
-- ============================================================================

comment on table member is '회원';
comment on column member.id is 'ID (UUID v7, 서버 채번)';
comment on column member.email is '이메일 (soft delete 된 행까지 포함해 유일)';
comment on column member.password is '암호화된 비밀번호';
comment on column member.nickname is '닉네임 (soft delete 된 행까지 포함해 유일)';
comment on column member.introduce is '자기소개';
comment on column member.status is '회원 상태 (REGISTER, ACTIVATE, INACTIVATE, SUSPENDED)';
comment on column member.created_at is '생성 일시';
comment on column member.modified_at is '수정 일시';
comment on column member.deleted_at is '삭제 일시 (soft delete)';

comment on table member_image is '회원 이미지';
comment on column member_image.id is 'ID (UUID v7, 서버 채번)';
comment on column member_image.member_id is '회원 ID';
comment on column member_image.type is '이미지 유형 (PROFILE)';
comment on column member_image.path is '원본 이미지 저장 경로';
comment on column member_image.name is '서버에서 변경해 저장한 원본 파일명';
comment on column member_image.origin_file_name is '사용자가 첨부한 파일명';
comment on column member_image.size is '원본 이미지 크기 (byte)';
comment on column member_image.resize_file_path is '리사이즈 이미지 저장 경로';
comment on column member_image.resize_file_name is '서버가 생성한 리사이즈 파일명';
comment on column member_image.resize_file_size is '리사이즈 이미지 크기 (byte)';
comment on column member_image.created_at is '생성 일시';
comment on column member_image.modified_at is '수정 일시';
comment on column member_image.deleted_at is '삭제 일시 (soft delete)';

comment on table member_suspension is '회원 정지 이력';
comment on column member_suspension.id is 'ID (UUID v7, 서버 채번)';
comment on column member_suspension.member_id is '회원 ID';
comment on column member_suspension.suspended_at is '정지 시작 일시';
comment on column member_suspension.suspended_to is '정지 종료 일시';
comment on column member_suspension.reason is '정지 사유';
comment on column member_suspension.created_at is '생성 일시';
comment on column member_suspension.modified_at is '수정 일시';
comment on column member_suspension.deleted_at is '삭제 일시 (soft delete)';

comment on table follow is '팔로우 (hard delete, deleted_at 은 쓰지 않는다)';
comment on column follow.id is 'ID (UUID v7, 서버 채번)';
comment on column follow.follower_id is '팔로우하는 회원 ID';
comment on column follow.followee_id is '팔로우 당하는 회원 ID';
comment on column follow.created_at is '생성 일시';
comment on column follow.modified_at is '수정 일시';
comment on column follow.deleted_at is '삭제 일시 (공통 컬럼, 사용하지 않음)';

comment on table authority is '권한';
comment on column authority.id is 'ID (UUID v7, 서버 채번)';
comment on column authority.name is '권한명';
comment on column authority.code is '권한 코드 (MEMBER, ADMIN, MODERATOR)';
comment on column authority.created_at is '생성 일시';
comment on column authority.modified_at is '수정 일시';
comment on column authority.deleted_at is '삭제 일시 (soft delete)';

comment on table menu is '메뉴';
comment on column menu.id is 'ID (UUID v7, 서버 채번)';
comment on column menu.parent_menu_id is '부모 메뉴 ID (최상위 메뉴는 null)';
comment on column menu.name is '메뉴명';
comment on column menu.code is '메뉴 코드 (유일)';
comment on column menu.path is '메뉴 경로';
comment on column menu.created_at is '생성 일시';
comment on column menu.modified_at is '수정 일시';
comment on column menu.deleted_at is '삭제 일시 (soft delete)';

comment on table member_authority is '회원 권한';
comment on column member_authority.id is 'ID (UUID v7, 서버 채번)';
comment on column member_authority.member_id is '회원 ID';
comment on column member_authority.authority_id is '권한 ID';
comment on column member_authority.created_at is '생성 일시';
comment on column member_authority.modified_at is '수정 일시';
comment on column member_authority.deleted_at is '삭제 일시 (soft delete)';

comment on table permission is '권한별 메뉴 접근';
comment on column permission.id is 'ID (UUID v7, 서버 채번)';
comment on column permission.authority_id is '권한 ID';
comment on column permission.menu_id is '메뉴 ID';
comment on column permission.created_at is '생성 일시';
comment on column permission.modified_at is '수정 일시';
comment on column permission.deleted_at is '삭제 일시 (soft delete)';

comment on table permission_action is 'permission 에 허용된 액션';
comment on column permission_action.id is 'ID (UUID v7, 서버 채번)';
comment on column permission_action.permission_id is 'permission ID';
comment on column permission_action.action is '허용된 액션 (CREATE, READ, UPDATE, DELETE)';
comment on column permission_action.created_at is '생성 일시';
comment on column permission_action.modified_at is '수정 일시';
comment on column permission_action.deleted_at is '삭제 일시 (soft delete)';

comment on table agreement is '약관 (수정하지 않고 추가 또는 soft delete 만 한다)';
comment on column agreement.id is 'ID (UUID v7, 서버 채번)';
comment on column agreement.title is '약관 제목';
comment on column agreement.content is '약관 내용';
comment on column agreement.is_multiple is '복수 선택 여부 (true 는 checkbox, false 는 radio)';
comment on column agreement.created_at is '생성 일시';
comment on column agreement.modified_at is '수정 일시';
comment on column agreement.deleted_at is '삭제 일시 (soft delete)';

comment on table agreement_answer is '약관의 선택 항목';
comment on column agreement_answer.id is 'ID (UUID v7, 서버 채번)';
comment on column agreement_answer.agreement_id is '약관 ID';
comment on column agreement_answer.name is '선택 항목명';
comment on column agreement_answer.is_positive is '긍정적인 답변 여부 (배치와 통계 쿼리용 참고 값)';
comment on column agreement_answer.created_at is '생성 일시';
comment on column agreement_answer.modified_at is '수정 일시';
comment on column agreement_answer.deleted_at is '삭제 일시 (soft delete)';

comment on table member_agreement is '회원이 선택한 약관 항목';
comment on column member_agreement.id is 'ID (UUID v7, 서버 채번)';
comment on column member_agreement.member_id is '회원 ID';
comment on column member_agreement.agreement_answer_id is '선택한 약관 항목 ID';
comment on column member_agreement.created_at is '생성 일시 (동의 일시)';
comment on column member_agreement.modified_at is '수정 일시';
comment on column member_agreement.deleted_at is '삭제 일시 (soft delete)';
