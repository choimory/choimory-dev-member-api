package dev.choimory.member.api.common.util;

import java.util.concurrent.TimeUnit;

/**
 * TimeUnit 관련 표시 문자열을 제공하는 유틸리티입니다.
 */
public class TimeUnitUtil {

    private TimeUnitUtil() {}

    /**
     * TimeUnit을 한국어 단위명으로 변환합니다.
     *
     * @param unit 시간 단위
     * @return 한국어 단위명
     */
    public static String toKorean(TimeUnit unit) {
        return switch (unit) {
            case SECONDS -> "초";
            case MINUTES -> "분";
            case HOURS -> "시간";
            case DAYS -> "일";
            default -> unit.name();
        };
    }
}
