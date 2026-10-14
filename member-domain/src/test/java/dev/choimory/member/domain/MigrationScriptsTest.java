package dev.choimory.member.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * MigrationScripts 단위 테스트입니다.
 */
class MigrationScriptsTest {

    /**
     * 버전 파일명 규칙(V{숫자}__{설명}.sql)의 파일만 버전 오름차순으로 조회되는지 확인합니다.
     */
    @Test
    void filesAreVersionedAndSortedByVersion() {
        // 마이그레이션 SQL 파일을 조회한다.
        List<Path> files = MigrationScripts.files();

        // 한 개 이상 있고 모두 버전 파일명 규칙에 맞아야 한다.
        assertThat(files).isNotEmpty();
        assertThat(files).allMatch(path -> path.getFileName().toString().matches("V\\d+__.+\\.sql"));

        // 버전이 오름차순이어야 한다.
        List<Integer> versions = files.stream()
                .map(path -> Integer.parseInt(
                        path.getFileName().toString().split("__")[0].substring(1)))
                .toList();
        assertThat(versions).isSorted();
    }

    /**
     * 첫 마이그레이션이 V1__init_schema.sql인지 확인합니다.
     */
    @Test
    void firstMigrationIsInitSchema() {
        // 첫 번째 파일의 이름이 V1__init_schema.sql이어야 한다.
        assertThat(MigrationScripts.files().get(0).getFileName().toString()).isEqualTo("V1__init_schema.sql");
    }
}
