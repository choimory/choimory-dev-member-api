package dev.choimory.member.api.common.domain.dto

data class CommonValidateResult(val field: String, val rejectValue: Any?, val message: String?)
