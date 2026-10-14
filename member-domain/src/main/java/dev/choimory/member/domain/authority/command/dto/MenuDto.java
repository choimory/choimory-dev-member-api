package dev.choimory.member.domain.authority.command.dto;

import dev.choimory.member.domain.authority.command.entity.Menu;
import java.time.Instant;
import java.util.List;
import lombok.Builder;

/**
 * 메뉴 DTO입니다. Menu 엔티티와 1:1로 매칭됩니다.
 * 하위 메뉴를 하위 DTO 컬렉션으로 포함하며 부모 메뉴는 포함하지 않습니다. 조회하지 않은 경우 하위 메뉴는 null입니다.
 *
 * @param id 메뉴 ID
 * @param name 메뉴명
 * @param code 메뉴 코드
 * @param path 메뉴 경로
 * @param children 하위 메뉴
 * @param createdAt 생성 일시
 * @param modifiedAt 수정 일시
 * @param deletedAt 삭제 일시
 */
@Builder(toBuilder = true)
public record MenuDto(
        String id,
        String name,
        String code,
        String path,
        List<MenuDto> children,
        Instant createdAt,
        Instant modifiedAt,
        Instant deletedAt) {

    /**
     * 메뉴 엔티티를 하위 메뉴까지 포함한 DTO로 변환합니다. 지연 로딩 컬렉션을 읽으므로 트랜잭션 안에서 호출해야 합니다.
     *
     * @param menu 메뉴 엔티티
     * @return 메뉴 DTO
     */
    public static MenuDto from(Menu menu) {
        // 메뉴의 필드를 변환하고 하위 메뉴를 하위 DTO로 변환한다.
        return fromWithoutChildren(menu).toBuilder()
                .children(menu.getChildren().stream().map(MenuDto::from).toList())
                .build();
    }

    /**
     * 메뉴 엔티티를 하위 메뉴 없이 메뉴의 필드만 DTO로 변환합니다.
     * children은 null이며 엔티티의 지연 로딩 컬렉션을 읽지 않습니다. 참조만 하는 관계(permission)에서 사용합니다.
     *
     * @param menu 메뉴 엔티티
     * @return 하위 메뉴가 null인 메뉴 DTO
     */
    public static MenuDto fromWithoutChildren(Menu menu) {
        return MenuDto.builder()
                .id(menu.getId())
                .name(menu.getName())
                .code(menu.getCode())
                .path(menu.getPath())
                .createdAt(menu.getCreatedAt())
                .modifiedAt(menu.getModifiedAt())
                .deletedAt(menu.getDeletedAt())
                .build();
    }
}
