package dev.choimory.member.domain;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 엔티티 매핑과 DDL의 정합성 테스트입니다.
 * ddl-auto=validate는 컬럼 타입만 비교하므로, 테이블 목록, 컬럼 목록, NOT NULL 여부가 DDL과 같은지 Hibernate가 실제로
 * 매핑한 정보와 information_schema를 비교해 확인합니다.
 */
@JpaEntityTest
class EntityMappingTest {

    @Autowired
    private EntityManager em; // 엔티티 매니저

    @Autowired
    private JdbcTemplate jdbc; // JDBC 템플릿

    /**
     * 엔티티가 매핑한 테이블 목록이 DDL의 테이블 목록과 같은지 확인합니다.
     */
    @Test
    void entityTablesMatchDdlTables() {
        // 엔티티가 매핑한 테이블과 DDL의 테이블을 조회한다.
        TreeSet<String> mappedTables = new TreeSet<>(mappedColumns().keySet());
        TreeSet<String> ddlTables = new TreeSet<>(jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema = current_schema()",
                String.class));

        // DDL의 모든 테이블에 엔티티가 있고 DDL에 없는 테이블을 매핑한 엔티티가 없어야 한다.
        assertThat(mappedTables).isEqualTo(ddlTables);
    }

    /**
     * 테이블마다 엔티티가 매핑한 컬럼 목록이 DDL의 컬럼 목록과 같은지 확인합니다.
     * (DDL에만 있는 컬럼을 엔티티가 빠뜨리거나, 엔티티에만 있는 컬럼이 없어야 한다)
     */
    @Test
    void entityColumnsMatchDdlColumns() {
        // 테이블마다 엔티티의 컬럼 목록과 DDL의 컬럼 목록을 비교한다.
        for (Map.Entry<String, Map<String, Boolean>> table : mappedColumns().entrySet()) {
            TreeSet<String> mapped = new TreeSet<>(table.getValue().keySet());
            TreeSet<String> ddl = new TreeSet<>(ddlColumns(table.getKey()).keySet());
            assertThat(mapped).as(table.getKey()).isEqualTo(ddl);
        }
    }

    /**
     * 컬럼마다 엔티티의 NOT NULL 여부가 DDL과 같은지 확인합니다.
     */
    @Test
    void entityNullabilityMatchesDdl() {
        // 컬럼마다 엔티티의 NULL 허용 여부와 DDL의 NULL 허용 여부를 비교한다.
        for (Map.Entry<String, Map<String, Boolean>> table : mappedColumns().entrySet()) {
            Map<String, Boolean> ddl = ddlColumns(table.getKey());
            for (Map.Entry<String, Boolean> column : table.getValue().entrySet()) {
                assertThat(column.getValue())
                        .as(table.getKey() + "." + column.getKey() + " 의 NULL 허용 여부")
                        .isEqualTo(ddl.get(column.getKey()));
            }
        }
    }

    /**
     * Hibernate가 실제로 매핑한 테이블별 컬럼과 NULL 허용 여부를 조회합니다.
     *
     * @return 테이블 이름, 컬럼 이름, NULL 허용 여부
     */
    private Map<String, Map<String, Boolean>> mappedColumns() {
        // Hibernate의 매핑 메타모델에서 엔티티별 컬럼을 모은다.
        SessionFactoryImplementor sessionFactory = em.getEntityManagerFactory().unwrap(SessionFactoryImplementor.class);
        Map<String, Map<String, Boolean>> tables = new TreeMap<>();
        sessionFactory.getMappingMetamodel().streamEntityDescriptors().forEach(descriptor -> {
            AbstractEntityPersister persister = (AbstractEntityPersister) descriptor;
            Map<String, Boolean> columns = tables.computeIfAbsent(persister.getTableName(), key -> new TreeMap<>());

            // 식별자 컬럼은 NULL을 허용하지 않는다.
            for (String column : persister.getIdentifierColumnNames()) {
                columns.put(column, false);
            }

            // 나머지 속성의 컬럼과 NULL 허용 여부를 모은다.
            String[] names = persister.getPropertyNames();
            boolean[] nullability = persister.getPropertyNullability();
            for (int i = 0; i < names.length; i++) {
                // 컬렉션(@OneToMany)은 이 테이블의 컬럼이 아니다. (소유자의 id 컬럼을 돌려주므로 제외한다)
                if (persister.getPropertyType(names[i]).isCollectionType()) {
                    continue;
                }
                String[] propertyColumns = persister.getPropertyColumnNames(names[i]);
                if (propertyColumns == null) {
                    continue;
                }
                for (String column : propertyColumns) {
                    if (column != null) {
                        columns.put(column, nullability[i]);
                    }
                }
            }
        });
        return tables;
    }

    /**
     * DDL의 테이블 컬럼과 NULL 허용 여부를 조회합니다.
     *
     * @param table 테이블 이름
     * @return 컬럼 이름, NULL 허용 여부
     */
    private Map<String, Boolean> ddlColumns(String table) {
        // information_schema에서 컬럼과 NULL 허용 여부를 조회한다.
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select column_name, is_nullable from information_schema.columns"
                        + " where table_schema = current_schema() and table_name = ?",
                table);
        Map<String, Boolean> columns = new TreeMap<>();
        for (Map<String, Object> row : rows) {
            columns.put((String) row.get("column_name"), "YES".equals(row.get("is_nullable")));
        }
        return columns;
    }
}
