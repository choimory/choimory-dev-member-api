package dev.choimory.member.api.member.v1.command.exception;

import dev.choimory.member.api.common.exception.CommonException;
import org.springframework.http.HttpStatus;

/**
 * 회원의 이메일 또는 닉네임이 이미 사용 중일 때 던지는 예외입니다.
 * soft delete된 회원의 값도 hard delete 배치 전까지는 사용 중으로 봅니다.
 */
public class MemberDuplicateException extends CommonException {

    public static final String EMAIL_CONSTRAINT = "uk_member_email"; // 이메일 유니크 제약명
    public static final String NICKNAME_CONSTRAINT = "uk_member_nickname"; // 닉네임 유니크 제약명

    /**
     * 응답 값을 지정해 중복 예외를 생성합니다.
     *
     * @param codeName 응답 코드명
     * @param msg 응답 메시지
     */
    private MemberDuplicateException(String codeName, String msg) {
        super(HttpStatus.CONFLICT, HttpStatus.CONFLICT.value(), codeName, msg);
    }

    /**
     * 이메일 중복 예외를 생성합니다.
     *
     * @return 이메일 중복 예외
     */
    public static MemberDuplicateException email() {
        return new MemberDuplicateException("DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다.");
    }

    /**
     * 닉네임 중복 예외를 생성합니다.
     *
     * @return 닉네임 중복 예외
     */
    public static MemberDuplicateException nickname() {
        return new MemberDuplicateException("DUPLICATE_NICKNAME", "이미 사용 중인 닉네임입니다.");
    }
}
