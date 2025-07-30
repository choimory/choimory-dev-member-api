package io.choimory.member.external.api.common.util

import java.util.concurrent.TimeUnit

class TimeUnitUtil {
    companion object {
        fun toKorean(unit:TimeUnit): String =
            when (unit) {
                TimeUnit.SECONDS -> "초"
                TimeUnit.MINUTES -> "분"
                TimeUnit.HOURS -> "시간"
                TimeUnit.DAYS -> "일"
                else -> unit.name
            }
    }
}