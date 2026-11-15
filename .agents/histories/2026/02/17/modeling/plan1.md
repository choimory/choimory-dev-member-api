# 목차

- [작업사항](#작업사항)
- [DB](#db)
- [DB2](#db2)
- [논의사항1](#논의사항1)
- [변경사항 재확인](#변경사항-재확인)
- [논의사항 1~11에 대한 의견](#논의사항-111에-대한-의견)
- [추가논의2](#추가논의2)
- [추가논의 1~7에 대한 의견](#추가논의-17에-대한-의견)
- [추가 논의3](#추가-논의3)
- [추가 논의3에 대한 의견](#추가-논의3에-대한-의견)
- [추가논의4](#추가논의4)
- [추가논의4에 대한 의견](#추가논의4에-대한-의견)
- [추가논의5](#추가논의5)
- [추가논의5에 대한 의견](#추가논의5에-대한-의견)

---

# 작업사항

- 회원 DB 설계

---

# DB

- id varchar, created_at datetime, modified_at datetime, deleted_at datetime은 모든 테이블 공통컬럼
- id는 uuid v7을 사용할거임
- 모든 엔티티는 deleted_at을 이용한 soft delete이며, 특별한 경우에만 비즈니스 로직에서 hard delete함

## 테이블

- member (email varchar, password varchar, nickname varchar, introduce text, status enum:REGISTER|ACTIVATE|INACTIVATE|SUSPENDED)
  - 1:N member_image (member_id varchar, type enum:PROFILE, path varchar, name varchar, origin_file_name varchar, size bigint, resize_file_path varchar, resize_file_name varchar, resize_file_size bigint)
  - N:N member_agreement (member_id varchar, agreement_id varchar, agreement_answer_id varchar)
  - N:N follow (id varchar, follower_id varchar, followee_id varchar)
  - 1:N member_suspension (member_id varchar, suspended_at datetime, suspended_to datetime, reason varchar)
  - N:N member_authority (member_id varchar, authority_id varchar)
- authority (name varchar, code enum:MEMBER|ADMIN|MODERATOR)
  - N:N permission (authority_id varchar, menu_id varchar)
- menu (parent_menu_id varchar, name varchar, code varchar, path varchar)
- agreement (type enum, title varchar, content text, is_multiple boolean)
  - agreement_answer (agreement_id varchar, name varchar, is_positive boolean)

---

# DB2

## 공통

- id(uuid v7), created_at, modified_at, deleted_at은 모든 테이블 공통컬럼
- 모든 엔티티는 deleted_at으로 soft delete
- 예외: follow는 hard delete, member는 탈퇴 후 배치로 hard delete (삭제 범위는 배치 작성 시 확정)
- enum은 DB와 서버 모두 enum으로 처리 (변동이 잦은 값은 서버만 enum)
- 약관(agreement, agreement_answer)은 수정하지 않고 추가 또는 soft delete

## 테이블

- member (email UNIQUE, password, nickname UNIQUE, introduce text, status enum:REGISTER|ACTIVATE|INACTIVATE|SUSPENDED)
  - 1:N member_image (member_id, type enum:PROFILE, path, name, origin_file_name, size, resize_file_path, resize_file_name, resize_file_size)
  - N:N member_agreement (member_id, agreement_answer_id) UNIQUE(member_id, agreement_answer_id)
  - N:N follow (id, follower_id, followee_id) UNIQUE(follower_id, followee_id), CHECK(follower_id <> followee_id)
  - 1:N member_suspension (member_id, suspended_at datetime, suspended_to datetime, reason)
  - N:N member_authority (member_id, authority_id)
- authority (name, code enum:MEMBER|ADMIN|MODERATOR)
  - N:N permission (authority_id, menu_id)
    - 1:N permission_action (permission_id, action enum:CREATE|READ|UPDATE|DELETE) UNIQUE(permission_id, action)
- menu (parent_menu_id, name, code, path)
- agreement (type enum, title, content text, is_multiple boolean)
  - 1:N agreement_answer (agreement_id, name, is_positive boolean)

## 삭제 정책

- member 탈퇴: member.deleted_at만 설정, 하위 행은 유지. 철회는 deleted_at을 null로 복구 (hard delete 전까지)
- hard delete 시 하위 테이블은 FK ON DELETE CASCADE, 다른 MSA의 member_id는 "존재하지 않거나 탈퇴한 회원"으로 처리

---

# 논의사항1

1. member_image의 origin_file_name과 name은 다른거임. origin_file_name은 사용자가 첨부한 파일의 파일명이고, name은 서버에서 변경해서 저장한 파일명임
2. path도 resize_file_path와 path를 따로 둘거임. 이미지별로 리사이즈 이미지를 서버에서 생성할것이기 때문에 원본 이미지별로 서버가 생성한 리사이즈 이미지도 존재하게 될것이기 때문임
3. n:n 관계 테이블도 id 컬럼을 둘거임 공통사항을 따름
4. agreed_at은 created_at, modified_at을 쓰면 될거 같은데 별도로 필요한 이유가 있을까
5. 자기 팔로우 방지 해야함. soft delete로 할지 hard delete로 할지는 큰 차이있을까? 필요한대로 하면 될듯
6. suspension datetime으로 변경
7. permission 메뉴별 crud 구분 권한 필요할듯?
8. member email, nickname 유니크제약 할거임. 이메일은 soft delete 이메일 중복 가능해야 할거고 닉네임은 불가능해야할거같은데 이러면 이메일이 테이블에 여러개 들어갈 수 있어서 로직에 곤란함이 생기려나?
9. authority.code는 varchar로 하되 백엔드에서만 enum으로 관리할까? 근데 어차피 권한은 사이트의 권한이라 관리자페이지 통해서 동적으로 생성되지 않고 그냥 엔지니어 거ㅓ쳐서 어렵게 해도 괜찮을거 같은데 그냥 enum으로 해도 되지 않나
10. is_positive는 그냥 답변이 긍정적인 답변인지 부정정인 답변인지 참고용으로 추가한 컬럼임. 배치나 이런 쿼리에 활용할 수 있지 않을까 싶어서
11. HEADER 제거한건 안쓸거같아서 제거했음. type은 향후 어떻게 변할지 모르니까 확장성 생각하면 유지가 맞을듯

--

# 변경사항 재확인

> 다시 확인

## 답변

처음 읽은 뒤에 plan1.md가 수정되어, 최신 내용 기준으로 변경점을 다시 정리함.

## 이번에 바뀐 부분

1. 공통 규칙에 soft delete 원칙 추가. 모든 엔티티는 deleted_at으로 soft delete하고, 특별한 경우에만 비즈니스 로직에서 hard delete함
2. member_image.type이 PROFILE만 남음 (HEADER 제거). 컬럼 순서도 origin_file_name이 name 바로 뒤로 이동
3. member_agreement에 agreement_answer_id 추가 (당시 오타: agreement_asnwer_id)
4. agreement에 is_multiple boolean 추가
5. 하위 테이블 agreement_answer (agreement_id, name, is_positive) 신설

## 초기 분석 중 달라진 점

- 약관 동의 항목은 일부 해소됨. 동의 여부는 agreement_answer로 표현하는 구조
- soft delete 원칙이 생겼으므로 follow의 언팔로우 처리와 유니크 제약 문제는 이 원칙을 기준으로 정함

## 새로 확인이 필요한 점

1. agreement_asnwer_id 오타 (agreement_answer_id가 맞음)
2. is_multiple의 의미: 한 약관에 복수 답변을 허용하는 것인지, 그렇다면 member_agreement가 (회원, 약관, 답변) 단위로 여러 행을 갖는 구조가 되는지. agreement_id와 agreement_answer_id가 둘 다 필요한지(답변에서 약관을 알 수 있으므로 중복 가능성)
3. is_positive의 의미: 동의/거부를 나타내는 값인지, 필수 약관에서 거부 답변을 허용하는지
4. HEADER 제거가 의도한 것인지, type을 PROFILE만 남길 거면 컬럼 자체가 필요한지

--

# 논의사항 1~11에 대한 의견

> 문서 29번줄 확인

## 답변

29번줄은 `# 논의사항`이며, 11개 항목에 대한 의견은 아래와 같음. 오타(agreement_answer_id)와 suspension의 datetime 변경은 본문에 반영 완료 상태.

## 동의 (1, 2, 3, 6, 10, 11)

- 1, 2: origin_file_name(사용자 원본명)과 name(서버 저장명)은 목적이 다르므로 분리가 맞음. 원본과 리사이즈 파일 경로를 따로 두는 것도 맞음
- 3: N:N 테이블에도 공통 id를 두는 데 동의. 대신 (member_id, agreement_id) 같은 조합에는 유니크 제약이 필요
- 6: suspension을 datetime으로 변경
- 10: is_positive는 참고용 컬럼으로 두고 배치와 통계 쿼리에 활용
- 11: type은 확장성을 위해 유지

## 4. agreed_at 필요 여부

created_at으로 충분함. 단, 동의 내역을 update하지 않고 불변으로 다뤄야 함. 동의를 바꿀 때는 기존 행을 soft delete하고 새 행을 생성. 그렇지 않으면 created_at이 최초 동의 시점이라 의미가 흐려짐.

다만 약관 본문(agreement.content)이 수정되면 회원이 어떤 내용에 동의했는지 추적할 수 없음. 약관도 불변으로 두고 수정 시 새 행을 만들거나 version 컬럼을 추가하는 것을 권장. 필수 약관 구분용 is_required도 현재 없음.

## 5. follow의 soft/hard delete

차이가 있음. (follower_id, followee_id)에 유니크 제약을 걸면 soft delete된 행 때문에 재팔로우가 막힘. 해결책은 두 가지.

- hard delete (권장): 팔로우는 이력 가치가 낮고 데이터가 많이 쌓이는 테이블이라 "특별한 경우"로 hard delete하는 것이 단순함
- soft delete 후 재팔로우 시 같은 행의 deleted_at을 null로 되돌리는 방식

자기 팔로우 방지는 앱 검증에 DB `CHECK (follower_id <> followee_id)`를 같이 두면 안전.

## 7. permission 메뉴별 CRUD

can_create, can_read, can_update, can_delete boolean 4개를 permission에 두는 방식이 가장 단순함. 액션이 늘어날 가능성이 크면 action enum으로 행을 나누는 방법도 있음.

## 8. 이메일과 닉네임 유니크

- 닉네임은 soft delete된 행까지 포함해 단순 UNIQUE(nickname)
- 이메일은 UNIQUE(email, deleted_at)로 하면 안 됨. MySQL에서는 NULL끼리 서로 다른 값으로 취급하므로 활성 회원끼리 같은 이메일이 중복 가입될 수 있음. 대안은 두 가지.
  - 생성 컬럼 `active_email = IF(deleted_at IS NULL, email, NULL)`에 유니크 (DB가 MySQL인지 확인 필요)
  - 탈퇴 시 이메일을 마스킹하거나 변조해서 저장

같은 이메일의 행이 여러 개 생기는 것은 로직에 큰 문제가 없음. 조회 시 `deleted_at IS NULL` 조건만 일관되게 걸면 되며, JPA라면 `@SQLRestriction`으로 처리 가능.

## 9. authority.code는 enum으로

권한이 코드 배포를 거쳐서만 추가된다면 백엔드 enum으로 충분함. 다만 DB 컬럼은 varchar로 두고 백엔드에서 `@Enumerated(EnumType.STRING)`으로 관리하는 것을 권장. DB 네이티브 enum은 값 추가 시 ALTER가 필요해 번거로움. member.status, member_image.type 등 다른 enum 컬럼도 같은 방식이 일관됨.

## 아직 답이 없는 질문

- is_multiple이 "한 약관에 복수 답변 허용"이라는 뜻이 맞는지
- member_agreement에 agreement_id와 agreement_answer_id가 둘 다 필요한지 (답변에서 약관을 알 수 있으므로 단일 답변 약관의 조회를 단순하게 하려는 의도가 아니면 중복)

---

# 추가논의2

1. 그럼 agreement는 레코드 수정없이 add or soft delete로 가자
2. follow는 hard delete로 가자
3. permission 하위에 permission_action 테이블 추가해서 action enum별로 레코드 저장하는 방식으로 가자
4. 이메일이랑 닉네임 그냥 unique로 넣고 soft delete하자. 탈퇴하면 1개월 뒤 배치로 hard delete하는 식으로 하면 될듯?
5. 그래도 Db enum을 쓰는게 안전하지 않나? 서버에만 의존하기 좀 불안하지 않나
6. is_multiple은 약관이 radio인지 checkbox인지, 즉 단일인지 복수선택 가능인지임
7. member_agreement에 agreement_id, agreement_answer_id를 다 넣은 이유는 agreement는 주제고, agreement_answer는 해당 약관의 각각의 선택항목을 저장하는 테이블임. 그래서 memeber_agreement에 agreement_id랑 agreement_answer_id를 다 넣은건 어느 약관의 어느 선택항목을 선택했다는 뜻임

--

# 추가논의 1~7에 대한 의견

> 130번줄 확인

## 답변

130번줄은 `# 추가논의`이며, 7개 항목에 결정과 질문이 섞여 있음. 항목별 의견은 아래와 같음.

## 1. agreement는 수정 없이 추가 또는 soft delete

동의. 약관 본문이 바뀌면 새 행을 만들고 이전 행은 soft delete. 그러면 회원이 동의한 약관 행은 그대로 남아 어떤 내용에 동의했는지 추적 가능. agreement_answer도 같은 원칙을 적용해야 함. 약관이 새 행이 되면 답변 항목도 새 약관 행에 맞춰 새로 생성.

## 2. follow는 hard delete

동의. UNIQUE(follower_id, followee_id)와 CHECK (follower_id <> followee_id)를 같이 둠. 공통 컬럼 deleted_at이 이 테이블에서는 쓰이지 않으므로 soft delete 원칙의 예외로 문서에 명시해 두는 것이 좋음.

## 3. permission_action 테이블 추가

방향은 좋음. 구조 확정이 필요한 부분은 다음과 같음.

- permission_action (permission_id, action enum:CREATE|READ|UPDATE|DELETE)로 두고, 행이 있으면 허용하는 방식이 가장 단순
- UNIQUE(permission_id, action) 필요
- permission은 (authority_id, menu_id) 한 쌍이 되고, 그 아래에 허용 액션이 붙음
- action을 DB enum으로 할지 varchar로 할지는 5번 결론에 따라 결정

## 4. 이메일과 닉네임은 단순 unique, 탈퇴 1개월 뒤 배치로 hard delete

방향은 맞으나 아래 제약이 따름.

- 유예 기간 동안 재가입 불가: 이메일에 단순 unique를 걸면 탈퇴 후 1개월 동안 같은 이메일로 재가입할 수 없음. 의도된 정책이라면 문제없고 탈퇴 철회 기간으로도 쓸 수 있으므로 정책으로 확정 필요
- hard delete 시 연쇄 삭제: member를 hard delete하려면 member_image, member_agreement, member_authority, member_suspension, follow도 함께 정리해야 함. FK의 ON DELETE CASCADE를 쓸지, 배치에서 순서대로 삭제할지 결정 필요. 업로드된 이미지 파일 삭제도 포함
- 법적 보관 의무: 동의 이력이나 제재 이력은 보관 의무가 있을 수 있으므로, 1개월 뒤 지워도 되는지 확인 필요
- 이 흐름이면 앞서 제안한 active_email 생성 컬럼이나 이메일 변조는 필요 없어짐

## 5. DB enum이 더 안전하지 않나

| | DB enum | varchar + 앱 enum |
|---|---|---|
| 잘못된 값 방어 | DB가 막음 | 앱 외 경로(수동 쿼리, 다른 서비스)로는 못 막음 |
| 값 추가 | ALTER TABLE 필요, 대형 테이블에서는 부담 | 배포만으로 가능 |
| 순서 의존 | 값 순서 변경이나 삭제가 위험 | 없음 |

대안으로 varchar에 `CHECK (col IN (...))` 제약을 거는 방법이 있음. DB 방어는 유지하면서 변경은 enum보다 쉬우나, 값 추가 시 제약도 같이 변경해야 함.

이 프로젝트는 값이 거의 안 바뀌는 코드성 컬럼(status, type, action)이 대부분이고 외부에서 DB에 직접 쓰는 경로도 없어 보이므로, DB enum으로 가도 큰 문제는 없음. 값 추가는 드물게 하는 ALTER로 감수하겠다고 정하면 됨. DB가 MySQL이면 enum 값을 끝에 추가하는 ALTER는 비교적 가벼움.

## 6. is_multiple은 radio와 checkbox 구분

의도 확인. 이름이 의도를 충분히 드러내지는 않으므로 필요하면 `selection_type enum:SINGLE|MULTIPLE`로 바꾸는 방법도 있으나, 지금 이름도 불편하지는 않음. 단일 선택 약관은 member_agreement에 (member_id, agreement_id)당 활성 행이 1개여야 한다는 규칙을 서비스 로직에서 보장해야 함.

## 7. member_agreement에 두 ID가 모두 필요한 이유

"어느 약관의 어느 항목을 선택했는가"를 한 행에 담는 구조로 이해함. 확인할 점은 다음과 같음.

- agreement_answer가 이미 agreement_id를 갖고 있으므로, 이 둘이 서로 다른 약관을 가리키는 불일치 데이터가 생길 수 있음. 서비스 로직에서 검증하거나 DB 복합 FK로 방지 가능
- 유니크 제약은 (member_id, agreement_answer_id)가 자연스러움

## 정리

- 확정: 1, 2, 3(구조 세부는 확정 필요), 6, 7
- 더 정해야 할 것: 4의 재가입 정책과 hard delete 범위, 5의 enum 방식

---

# 추가 논의3

- 재가입은 hard delete 배치 돌기 전까지 재가입 불가 정책, hard delete 되면 fk cascade on delete로 일괄 삭제 (회원정보만. 다른 msa의 블로그 게시물 이런건 남기고)
  - 아니면 그냥 unique 풀고 조건을 email or nickname & deleted_at=null로 갈까? 이게 더 나은거 같은데 생각해보니
- enum은 대부분 내가 고려할때 거의 안바뀌는 내용들만 고려해서 정해준거라 큰 문제는 없음 
- member_agreement에 agreement_answer_id만 넣어도 될거같네 보니

--

# 추가 논의3에 대한 의견

> 219번줄 확인

## 답변

219번줄은 `# 추가 논의3`이며, 4개 항목에 대한 의견은 아래와 같음.

## 1. 재가입 정책

탈퇴 후 hard delete 배치 전까지 재가입을 막고, hard delete 때 FK cascade로 회원 정보만 지우고 MSA의 다른 서비스 데이터는 남기는 방향은 일관됨. 다만 아래를 정해야 함.

- 다른 서비스의 member_id 참조: 블로그 같은 다른 서비스에는 member_id가 남음. 회원 삭제 후 그 글의 작성자를 어떻게 표시할지 결정 필요 (예: "탈퇴한 회원"). 다른 서비스가 닉네임 같은 개인정보를 복사해 두고 있다면 그것도 지워야 하는지 확인 필요
- 파일 삭제: DB cascade는 파일을 지워주지 않음. 업로드된 프로필 이미지(원본과 리사이즈)는 배치에서 따로 삭제해야 함
- 보관 의무 데이터: 동의 이력과 제재 이력을 같이 지워도 되는지는 아직 미확인

## 2. unique를 풀고 `deleted_at IS NULL` 조건으로 가는 안

이 안이 낫다는 생각에는 동의하지 않음. 유니크를 DB가 아닌 앱 로직에만 맡기게 되기 때문. 동시에 두 가입 요청이 들어오면 둘 다 "중복 없음"으로 판단하고 같은 이메일이 두 번 저장될 수 있음. 이는 DB 제약이 방어해야 하는 부분.

- 이 안을 유지하면서 DB 방어도 두려면, `active_email = IF(deleted_at IS NULL, email, NULL)` 생성 컬럼에 유니크를 거는 방식(MySQL) 또는 부분 유니크 인덱스(PostgreSQL)가 필요. DB 종류 확인 필요
- 재가입을 막는 정책이 괜찮다면 단순 UNIQUE가 가장 안전하고 단순함. 탈퇴 철회 기간으로도 쓸 수 있음

"탈퇴 후 1개월 동안 같은 이메일로 재가입을 막아도 되는가"가 먼저 정해져야 함. 막아도 되면 단순 UNIQUE, 즉시 재가입이 필요하면 생성 컬럼 유니크를 권장. 닉네임은 어느 경우에도 단순 UNIQUE가 맞음.

## 3. enum

DB enum으로 확정. 코드성 컬럼이 대부분이고 거의 안 바뀐다는 전제에 동의. 값 추가는 드문 ALTER로 감수하는 것으로 정리.

## 4. member_agreement에 agreement_answer_id만

동의. 약관 행을 불변으로 두기로 했으므로 답변에서 약관은 항상 같게 정해짐. 장점은 다음과 같음.

- 앞서 우려한 agreement_id와 agreement_answer_id의 불일치 데이터가 구조적으로 생기지 않음
- UNIQUE(member_id, agreement_answer_id)로 중복 동의를 막을 수 있음

주의할 점: 단일 선택(radio) 약관에서 한 회원이 같은 약관의 서로 다른 답변 두 개를 동시에 선택하는 것은 DB 유니크로 막을 수 없음. 이 규칙은 서비스 로직에서 보장해야 하며, 약관별 동의 여부 조회 시 agreement_answer를 조인하게 됨.

## 정리

- 확정: 3(DB enum), 4(agreement_answer_id만 사용)
- 더 정해야 할 것: 재가입 정책(탈퇴 후 유예 기간 동안 재가입 차단 여부), DB 종류, hard delete 범위(보관 의무 데이터, 이미지 파일 삭제, 다른 서비스 참조 처리) 

---

# 추가논의4

1. hard delete전까지 재가입 막고, 다른 msa에 남은 member_id는 존재하지 않거나 탈퇴한 회원입니다로 안내 처리
2. 그냥 unique걸고 회원정보 hard delete 전까지 가입 못하게 하자
3. 변동이 적은건 db+서버 모두 enum, 변동이 있는건 서버만 enum 처리 (지금 설계한 테이블은 모두 db+서버 enum 처리)
4. 한 약관에 복수선택이면 한 약관에서 선택한 선택지 여러개가 정해지나? 그리고 이러면 n:N이 아니고 다른형태인가

--

# 추가논의4에 대한 의견

> 274번줄 확인

## 답변

274번줄은 `# 추가논의4`이며, 4개 항목에 대한 의견은 아래와 같음.

## 1. hard delete 전까지 재가입 차단, 다른 MSA의 member_id는 "존재하지 않거나 탈퇴한 회원"으로 안내

동의. 다른 서비스가 회원 서비스의 삭제 이벤트를 받아서 처리할 필요가 없어 단순함. 정해 둘 점은 다음과 같음.

- 다른 서비스가 회원 정보를 조회할 때 회원 서비스가 "없음"과 "탈퇴"를 구분해서 응답할지, 구분 없이 같은 응답을 줄지 결정 필요. 개인정보 노출을 줄이려면 구분 없이 같은 응답이 나음
- 다른 서비스가 닉네임 같은 개인정보를 복사해 저장해 두었는지는 미확인. 복사본이 있다면 그것도 지우는 흐름이 필요

## 2. 단순 unique, hard delete 전까지 재가입 불가

동의. 이 정책으로 확정하면 active_email 생성 컬럼은 필요 없고, DB 종류도 이 문제에서는 상관없어짐. 닉네임도 같은 방식이므로 탈퇴 회원의 닉네임도 hard delete 전까지 점유됨. 이에 맞춰 챙길 점은 다음과 같음.

- 로그인과 조회는 `deleted_at IS NULL`인 회원만 대상이어야 함
- 유예 기간 중 탈퇴 철회(복구)를 지원할지 결정 필요. 현재 설계로는 deleted_at을 null로 되돌리면 되므로 쉽게 지원 가능

## 3. enum 정책

"변동이 적으면 DB와 서버 모두 enum, 변동이 있으면 서버만 enum"이라는 기준은 명확하고, 현재 테이블은 전부 앞쪽에 해당한다는 판단에 동의. 확정되면 문서에 원칙으로 적어 두는 것이 좋음. 서버 enum에 값을 추가할 때 DB 마이그레이션도 같이 해야 한다는 점만 유의.

## 4. 복수선택 약관의 구조

복수선택이면 한 회원이 한 약관에서 고른 선택지 개수만큼 member_agreement에 행이 생김. 예를 들어 마케팅 수신 약관에서 이메일, SMS, 푸시 중 이메일과 푸시를 골랐다면 아래와 같음.

| member_id | agreement_answer_id |
|---|---|
| M1 | 이메일 |
| M1 | 푸시 |

N:N 구조가 맞음. 정확히는 member와 agreement_answer 사이의 N:N을 풀어준 연결 테이블이 member_agreement. agreement는 agreement_answer와 1:N이므로 회원과 약관은 답변을 통해 간접적으로 연결됨. agreement_answer_id만 두기로 했으므로 문서의 테이블 목록에서 member_agreement 아래에 약관 ID가 없다는 점이 반영되어야 함.

단일선택(radio)도 같은 구조이며, 그 경우 회원당 한 약관에서 행이 1개만 생기도록 서비스 로직에서 보장.

## 정리

- 확정: 1, 2, 3
- 남은 것: hard delete 범위(보관 의무 데이터, 이미지 파일 삭제), 탈퇴 철회 지원 여부
- `# DB` 섹션에 합의된 내용(follow hard delete 예외, permission_action, agreement_answer_id만 사용하는 구조, 유니크 제약) 반영 필요

---

# 추가논의5

- hard delete 범위는 배치 만들때 정하면 되고, 탈퇴철회는 지원할거임 그냥 deleted_at만 null로 돌리면 되잖아
- 변경할 db 내용을 Db 섹션 밑에 Db2로 하나 적어봐

--

# 추가논의5에 대한 의견

> 332번줄 확인

## 답변

332번줄은 `# 추가논의5`이며, 2개 항목에 대한 의견은 아래와 같음.

## 1. hard delete 범위는 배치 만들 때, 탈퇴철회는 지원

동의. hard delete 범위는 배치 구현 단계에서 정해도 늦지 않음. 다만 보관 의무 데이터와 이미지 파일 삭제는 그때 놓치기 쉬우므로 배치 작업의 체크리스트로 남겨 둘 것.

탈퇴철회는 member.deleted_at을 null로 되돌리면 되는 것이 맞음. 정해 둘 점은 다음과 같음.

- 탈퇴 시 member만 soft delete할지, 하위 테이블(member_image 등)까지 같이 soft delete할지 결정 필요. 하위 행은 그대로 두고 조회 시 회원을 기준으로 거르는 방식이 복구가 단순하므로 권장
- 복구 가능한 시점은 hard delete 배치 전까지. 배치가 지운 뒤에는 복구할 수 없다는 점을 정책으로 명시

## 2. 변경된 DB 내용을 DB 섹션 밑에 DB2로 작성

`# DB` 섹션 바로 아래, `# 논의사항1` 앞에 `# DB2`를 추가함. 기존 `# DB` 내용은 그대로 두었고, 목차에도 DB2를 추가함. 구성은 공통, 테이블, 삭제 정책.

## 초안에서 아직 합의되지 않은 부분

- member_authority, permission의 UNIQUE 제약은 제안만 한 상태로 DB2에 넣지 않음
- agreement의 version, is_required는 합의된 것이 없어 DB2에서 제외
- follow는 hard delete라서 공통 컬럼 deleted_at이 쓰이지 않음. 이 테이블에서 deleted_at을 뺄지, 그대로 둘지 결정 필요
- "탈퇴 후 1개월 뒤 배치"의 1개월은 확정이 아니라서 DB2에 기간을 적지 않음