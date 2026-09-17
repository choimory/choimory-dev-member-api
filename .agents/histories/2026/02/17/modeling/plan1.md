# 작업사항

- DB 설계

---

# DB

> id varchar, created_at datetime, modified_at datetime, deleted_at datetime은 모든 테이블 공통컬럼

- member (email varchar, password varchar, nickname varchar, introduce text, status enum:REGISTER, ACTIVATE, INACTIVATE, SUSPENDED)
    - 1:N member_image (member_id varchar, type enum:PROFILE, HEADER, path varchar, name varchar, size bigint, resize_file_path varchar, origin_file_name varchar, resize_file_name varchar, resize_file_size bigint)
    - N:N member_agreement (member_id varchar, agreement_id varchar)
    - N:N follow (id varchar, follower_id varchar, followee_id varchar)
    - 1:N member_suspension (suspended_at date, suspended_to date, reason varchar)
    - N:N member_authority (member_id varchar, authority_id varchar)
- agreement (type enum, title varchar, content text)
- authority (name varchar, code enum:MEMBER, ADMIN, MODERATOR)
    - N:N permission (authority_id varchar, menu_id varchar)
- menu (parent_menu_id varchar, name varchar, code varchar, path varchar)