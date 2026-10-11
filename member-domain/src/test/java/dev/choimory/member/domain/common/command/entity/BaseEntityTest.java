package dev.choimory.member.domain.common.command.entity;

import static org.assertj.core.api.Assertions.assertThat;

import dev.choimory.member.domain.JpaAuditingTestConfiguration;
import dev.choimory.member.domain.TestcontainersConfiguration;
import dev.choimory.member.testsupport.TestBaseEntity;
import dev.choimory.member.testsupport.TestBaseEntityRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

/**
 * BaseEntity 테스트입니다.
 * 테스트 전용 엔티티(TestBaseEntity)를 통해 id 채번, Persistable, Auditing, soft delete 필터를 검증합니다.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaAuditingTestConfiguration.class})
@Sql(
        statements = "create table if not exists test_base_entity ("
                + "id varchar(36) not null primary key, name varchar(100),"
                + " created_at timestamp not null, modified_at timestamp, deleted_at timestamp)")
class BaseEntityTest {

    /**
     * 테스트 전용 엔티티와 레포지토리만 스캔하는 설정입니다.
     */
    @TestConfiguration
    @EntityScan(basePackageClasses = TestBaseEntity.class)
    @EnableJpaRepositories(basePackageClasses = TestBaseEntityRepository.class)
    static class ScanConfig {}

    @Autowired
    private EntityManager em; // 엔티티 매니저

    @Autowired
    private TestBaseEntityRepository repository; // 테스트 전용 레포지토리

    @Autowired
    private JdbcTemplate jdbc; // JDBC 템플릿

    /**
     * 테스트마다 테이블을 비웁니다.
     */
    @BeforeEach
    void cleanTable() {
        // 이전 테스트의 데이터를 삭제한다.
        jdbc.update("delete from test_base_entity");
    }

    // ---------------------------------------------------------------------------------------------
    // id 채번
    // ---------------------------------------------------------------------------------------------

    /**
     * 새 엔티티의 id가 UUID v7 형식(36자)으로 채번되는지 확인합니다.
     */
    @Test
    void newEntityHasUuidV7Id() {
        // 새 엔티티를 생성한다.
        TestBaseEntity entity = TestBaseEntity.create("name");

        // id는 36자 UUID이며 버전이 7이어야 한다.
        assertThat(entity.getId()).hasSize(36);
        assertThat(UUID.fromString(entity.getId()).version()).isEqualTo(7);
    }

    /**
     * 나중에 만든 엔티티의 id가 먼저 만든 엔티티의 id보다 사전식으로 뒤인지(시간 순서) 확인합니다.
     *
     * @throws InterruptedException 대기 중 인터럽트된 경우 발생
     */
    @Test
    void idsAreTimeOrdered() throws InterruptedException {
        // 시간 간격을 두고 두 엔티티를 생성한다.
        TestBaseEntity first = TestBaseEntity.create("first");
        Thread.sleep(5);
        TestBaseEntity second = TestBaseEntity.create("second");

        // 나중에 만든 id가 사전식으로 뒤여야 한다.
        assertThat(second.getId()).isGreaterThan(first.getId());
    }

    /**
     * 어떤 생성자로 만들어도(JPA가 사용하는 기본 생성자 포함) id가 UUID v7로 채번되는지 확인합니다.
     * 채번을 BaseEntity의 기본 생성자 한 곳에서 하므로 Lombok이 만든 생성자로 만들어도 빠뜨릴 수 없습니다.
     * (DB에서 읽을 때도 생성되지만 곧바로 DB 값으로 덮어씁니다)
     *
     * @throws ReflectiveOperationException 리플렉션 호출에 실패한 경우 발생
     */
    @Test
    void jpaConstructorAlsoGeneratesId() throws ReflectiveOperationException {
        // 기본 생성자로 엔티티를 생성한다.
        var constructor = TestBaseEntity.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        TestBaseEntity entity = constructor.newInstance();

        // id가 UUID v7로 채번되어 있어야 한다.
        assertThat(entity.getId()).hasSize(36);
        assertThat(UUID.fromString(entity.getId()).version()).isEqualTo(7);
    }

    /**
     * DB에서 읽은 엔티티의 id는 생성자에서 만든 값이 아니라 DB에 저장된 값인지 확인합니다.
     */
    @Test
    void loadedEntityKeepsStoredId() {
        // 엔티티를 저장하고 영속성 컨텍스트를 비운 뒤 다시 조회한다.
        TestBaseEntity entity = TestBaseEntity.create("name");
        String storedId = entity.getId();
        em.persist(entity);
        em.flush();
        em.clear();
        TestBaseEntity loaded = em.find(TestBaseEntity.class, storedId);

        // 조회한 엔티티의 id는 저장된 id와 같아야 한다.
        assertThat(loaded.getId()).isEqualTo(storedId);
    }

    // ---------------------------------------------------------------------------------------------
    // Persistable
    // ---------------------------------------------------------------------------------------------

    /**
     * 새로 만든 엔티티는 isNew가 true인지 확인합니다.
     */
    @Test
    void newEntityIsNew() {
        // 새 엔티티를 생성한다.
        TestBaseEntity entity = TestBaseEntity.create("name");

        // 저장 전에는 새 엔티티여야 한다.
        assertThat(entity.isNew()).isTrue();
    }

    /**
     * 저장 후에는 isNew가 false로 바뀌는지 확인합니다.
     */
    @Test
    void entityIsNotNewAfterPersist() {
        // 엔티티를 저장한다.
        TestBaseEntity entity = TestBaseEntity.create("name");
        em.persist(entity);
        em.flush();

        // 저장 후에는 새 엔티티가 아니어야 한다.
        assertThat(entity.isNew()).isFalse();
    }

    /**
     * DB에서 읽은 엔티티는 isNew가 false인지 확인합니다.
     */
    @Test
    void loadedEntityIsNotNew() {
        // 엔티티를 저장하고 영속성 컨텍스트를 비운 뒤 다시 조회한다.
        TestBaseEntity entity = TestBaseEntity.create("name");
        em.persist(entity);
        em.flush();
        em.clear();
        TestBaseEntity loaded = em.find(TestBaseEntity.class, entity.getId());

        // 조회한 엔티티는 새 엔티티가 아니어야 하고, id가 그대로여야 한다.
        assertThat(loaded.isNew()).isFalse();
        assertThat(loaded.getId()).isEqualTo(entity.getId());
    }

    /**
     * ID를 미리 채번한 새 엔티티를 save해도 추가 SELECT 없이 INSERT만 실행되는지 확인합니다.
     */
    @Test
    void saveExecutesOnlyInsertWithoutSelect() {
        // 통계를 초기화한다.
        Statistics statistics = em.unwrap(Session.class).getSessionFactory().getStatistics();
        statistics.clear();

        // id를 미리 채번한 새 엔티티를 save한다.
        repository.save(TestBaseEntity.create("name"));
        em.flush();

        // SELECT 없이 INSERT 한 번만 실행되어야 한다.
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        assertThat(statistics.getEntityInsertCount()).isEqualTo(1);
    }

    // ---------------------------------------------------------------------------------------------
    // Auditing
    // ---------------------------------------------------------------------------------------------

    /**
     * 저장 시 createdAt만 채워지고 수정 이력이 없는 modifiedAt은 null인지 확인합니다.
     */
    @Test
    void auditingFillsOnlyCreatedAtOnPersist() {
        // 엔티티를 저장한다.
        TestBaseEntity entity = TestBaseEntity.create("name");
        em.persist(entity);
        em.flush();

        // 생성 일시만 채워지고 수정 일시와 삭제 일시는 비어 있어야 한다.
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getModifiedAt()).isNull();
        assertThat(entity.getDeletedAt()).isNull();
    }

    /**
     * 처음 수정할 때 modifiedAt이 채워지고 이후 수정마다 갱신되며 createdAt은 유지되는지 확인합니다.
     *
     * @throws InterruptedException 대기 중 인터럽트된 경우 발생
     */
    @Test
    void modifiedAtIsFilledOnUpdateAndCreatedAtDoesNotChange() throws InterruptedException {
        // 엔티티를 저장하고 다시 조회한다.
        TestBaseEntity entity = TestBaseEntity.create("name");
        em.persist(entity);
        em.flush();
        em.clear();
        TestBaseEntity loaded = em.find(TestBaseEntity.class, entity.getId());
        LocalDateTime createdAt = loaded.getCreatedAt();

        // 수정 이력이 없으므로 modifiedAt은 null이어야 한다.
        assertThat(loaded.getModifiedAt()).isNull();

        // 시간 간격을 두고 처음 수정한다.
        Thread.sleep(10);
        loaded.rename("changed");
        em.flush();

        // 수정 일시가 채워지고 생성 일시는 유지되어야 한다.
        LocalDateTime firstModifiedAt = loaded.getModifiedAt();
        assertThat(loaded.getCreatedAt()).isEqualTo(createdAt);
        assertThat(firstModifiedAt).isAfter(createdAt);

        // 시간 간격을 두고 다시 수정하면 수정 일시가 갱신되어야 한다.
        Thread.sleep(10);
        loaded.rename("changed again");
        em.flush();
        assertThat(loaded.getModifiedAt()).isAfter(firstModifiedAt);
    }

    // ---------------------------------------------------------------------------------------------
    // soft delete
    // ---------------------------------------------------------------------------------------------

    /**
     * soft delete하면 deletedAt이 채워지는지 확인합니다.
     */
    @Test
    void softDeleteSetsDeletedAt() {
        // 엔티티를 soft delete한다.
        TestBaseEntity entity = TestBaseEntity.create("name");
        entity.softDelete();

        // 삭제 일시가 채워져야 한다.
        assertThat(entity.getDeletedAt()).isNotNull();
    }

    /**
     * soft delete된 행은 JPQL 조회에서 제외되는지 확인합니다.
     * (@SQLRestriction이 BaseEntity(@MappedSuperclass)에서 하위 엔티티로 상속되는지도 함께 검증)
     */
    @Test
    void softDeletedRowIsExcludedFromJpql() {
        // 엔티티를 저장하고 soft delete한 뒤 영속성 컨텍스트를 비운다.
        persistSoftDeletedEntity();

        // JPQL 조회에서 제외되어야 한다.
        List<TestBaseEntity> result = em.createQuery("select t from TestBaseEntity t", TestBaseEntity.class)
                .getResultList();
        assertThat(result).isEmpty();
    }

    /**
     * soft delete된 행은 레포지토리 목록 조회에서 제외되는지 확인합니다.
     */
    @Test
    void softDeletedRowIsExcludedFromRepositoryFindAll() {
        // 엔티티를 저장하고 soft delete한 뒤 영속성 컨텍스트를 비운다.
        persistSoftDeletedEntity();

        // 목록 조회에서 제외되어야 한다.
        assertThat(repository.findAll()).isEmpty();
    }

    /**
     * soft delete된 행은 id로 직접 조회(find, findById)에서도 제외되는지 확인합니다.
     */
    @Test
    void softDeletedRowIsExcludedFromFindById() {
        // 엔티티를 저장하고 soft delete한 뒤 영속성 컨텍스트를 비운다.
        TestBaseEntity entity = persistSoftDeletedEntity();

        // id로 직접 조회해도 제외되어야 한다.
        Optional<TestBaseEntity> found = repository.findById(entity.getId());
        assertThat(found).isEmpty();
        assertThat(em.find(TestBaseEntity.class, entity.getId())).isNull();
    }

    /**
     * 삭제되지 않은 행은 조회되는지 확인합니다.
     */
    @Test
    void notDeletedRowIsVisible() {
        // 엔티티를 저장한다.
        TestBaseEntity entity = TestBaseEntity.create("name");
        em.persist(entity);
        em.flush();
        em.clear();

        // 조회되어야 한다.
        assertThat(repository.findById(entity.getId())).isPresent();
    }

    /**
     * soft delete된 행도 네이티브 쿼리로는 조회되는지 확인합니다. (탈퇴철회, hard delete 배치, 중복 확인용)
     */
    @Test
    void softDeletedRowIsVisibleToNativeQuery() {
        // 엔티티를 저장하고 soft delete한다.
        TestBaseEntity entity = TestBaseEntity.create("name");
        em.persist(entity);
        entity.softDelete();
        em.flush();
        em.clear();

        // 네이티브 쿼리로는 삭제된 행을 조회할 수 있어야 한다.
        Number count = (Number)
                em.createNativeQuery("select count(*) from test_base_entity where id = :id and deleted_at is not null")
                        .setParameter("id", entity.getId())
                        .getSingleResult();
        assertThat(count.intValue()).isEqualTo(1);
    }

    /**
     * 엔티티를 저장하고 soft delete한 뒤 영속성 컨텍스트를 비웁니다.
     *
     * @return soft delete된 엔티티
     */
    private TestBaseEntity persistSoftDeletedEntity() {
        // 엔티티를 저장하고 soft delete한다.
        TestBaseEntity entity = TestBaseEntity.create("name");
        em.persist(entity);
        entity.softDelete();
        em.flush();
        em.clear();
        return entity;
    }
}
