package dev.choimory.member.api.authority.v1.command.handler;

import dev.choimory.member.api.authority.v1.command.repository.AuthorityRepository;
import dev.choimory.member.domain.authority.command.dto.AuthorityDto;
import dev.choimory.member.domain.authority.command.type.AuthorityCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 권한 명령의 세부 로직을 담당하는 핸들러입니다.
 */
@Component
@RequiredArgsConstructor
public class AuthorityCommandHandler {

    private final AuthorityRepository authorityRepository; // 권한 데이터 접근 객체

    /**
     * 권한 코드로 권한을 조회합니다. 권한은 코드 배포와 함께 데이터가 준비되어야 하므로 없으면 서버 오류입니다.
     *
     * @param code 권한 코드
     * @return 하위 컬렉션이 없는 권한 DTO
     * @throws IllegalStateException 권한 데이터가 없는 경우
     */
    public AuthorityDto findByCode(AuthorityCode code) {
        return authorityRepository
                .findByCode(code)
                .map(AuthorityDto::fromWithoutChildren)
                .orElseThrow(() -> new IllegalStateException("권한 데이터가 없습니다. code=" + code));
    }
}
