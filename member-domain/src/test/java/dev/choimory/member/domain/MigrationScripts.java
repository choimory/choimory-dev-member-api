package dev.choimory.member.domain;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * docs/flyway의 마이그레이션 SQL 파일을 버전 순으로 조회하는 테스트 지원 클래스입니다.
 * member-domain은 Flyway를 쓰지 않으므로 테스트에서 직접 SQL 파일을 순서대로 적용할 때 사용합니다.
 */
public final class MigrationScripts {

    private static final Pattern VERSIONED = Pattern.compile("V(\\d+)__.+\\.sql"); // 버전 마이그레이션 파일명 규칙

    private MigrationScripts() {}

    /**
     * 시스템 프로퍼티 docs.flyway.dir 아래의 마이그레이션 SQL 파일을 버전 오름차순으로 조회합니다.
     *
     * @return 버전 순으로 정렬된 마이그레이션 SQL 파일 경로
     * @throws UncheckedIOException 디렉토리를 읽지 못한 경우 발생
     */
    public static List<Path> files() {
        // 마이그레이션 디렉토리 아래의 파일 중 버전 파일명 규칙에 맞는 것만 모은다.
        Path dir = Paths.get(System.getProperty("docs.flyway.dir"));
        try (Stream<Path> paths = Files.walk(dir)) {
            // 파일명의 버전 숫자 순으로 정렬한다. (V10이 V2보다 뒤에 오도록 숫자로 비교한다)
            return paths.filter(path ->
                            VERSIONED.matcher(path.getFileName().toString()).matches())
                    .sorted(Comparator.comparingInt(MigrationScripts::version))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 마이그레이션 파일명에서 버전 숫자를 추출합니다.
     *
     * @param path 마이그레이션 SQL 파일 경로
     * @return 버전 숫자
     */
    private static int version(Path path) {
        // V{버전}__{설명}.sql에서 버전을 꺼낸다.
        Matcher matcher = VERSIONED.matcher(path.getFileName().toString());
        matcher.matches();
        return Integer.parseInt(matcher.group(1));
    }
}
