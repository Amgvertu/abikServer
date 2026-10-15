package info.prorabka.abik.server.charge.controller;

import info.prorabka.abik.server.charge.dto.ChargeGroupRequest;
import info.prorabka.abik.server.charge.dto.ChargeGroupResponse;
import info.prorabka.abik.server.charge.dto.ChargeSubgroupRequest;
import info.prorabka.abik.server.charge.dto.ChargeSubgroupResponse;
import info.prorabka.abik.server.charge.service.ChargeGroupService;
import info.prorabka.abik.server.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Группы и подгруппы начислений внутри команды.
 */
@RestController
@RequestMapping("/v1/teams/{teamId}/charge-groups")
@Tag(name = "Charge Groups",
        description = """
             Группы и подгруппы начислений внутри команды.
             Примеры групп: «Лёд», «Форма», «Взносы».
             Примеры подгрупп: «Основной состав», «Вратари».

             **Права:**
             - Чтение — активные участники команды.
             - Создание/изменение/удаление — владелец или модератор.
             """)
@SecurityRequirement(name = "bearerAuth")
public class ChargeGroupsController {

    private final ChargeGroupService groupService;

    public ChargeGroupsController(ChargeGroupService groupService) {
        this.groupService = groupService;
    }

    // ============================================================
    // Группы
    // ============================================================

    @Operation(summary = "Список групп", description = "Все группы команды, отсортированные по названию.")
    @ApiResponse(responseCode = "200", description = "Список групп",
            content = @Content(schema = @Schema(implementation = ChargeGroupResponse.class)))
    @GetMapping
    public ResponseEntity<List<ChargeGroupResponse>> listGroups(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(groupService.listGroups(teamId, userId));
    }

    @Operation(summary = "Создать группу",
            description = "Название должно быть уникальным внутри команды. **Доступ:** владелец/модератор.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Группа создана",
                    content = @Content(schema = @Schema(implementation = ChargeGroupResponse.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "409", description = "Группа с таким названием уже существует")
    })
    @PostMapping
    public ResponseEntity<ChargeGroupResponse> createGroup(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @Valid @RequestBody ChargeGroupRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(groupService.createGroup(teamId, userId, req));
    }

    @Operation(summary = "Переименовать группу", description = "**Доступ:** владелец/модератор.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Группа переименована",
                    content = @Content(schema = @Schema(implementation = ChargeGroupResponse.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Группа не найдена"),
            @ApiResponse(responseCode = "409", description = "Название уже занято")
    })
    @PutMapping("/{groupId}")
    public ResponseEntity<ChargeGroupResponse> renameGroup(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @Parameter(description = "ID группы", example = "1") @PathVariable Long groupId,
            @Valid @RequestBody ChargeGroupRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(groupService.renameGroup(teamId, groupId, userId, req));
    }

    @Operation(summary = "Удалить группу",
            description = """
                    Удаление невозможно, если к группе привязаны шаблоны или начисления.
                    **Доступ:** владелец/модератор.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Группа удалена"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Группа не найдена"),
            @ApiResponse(responseCode = "409", description = "Есть зависимости (шаблоны/начисления)")
    })
    @DeleteMapping("/{groupId}")
    public ResponseEntity<Map<String, String>> deleteGroup(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @Parameter(description = "ID группы", example = "1") @PathVariable Long groupId) {
        Long userId = SecurityUtils.getCurrentUserId();
        groupService.deleteGroup(teamId, groupId, userId);
        return ResponseEntity.ok(Map.of("message", "Группа удалена"));
    }

    // ============================================================
    // Подгруппы
    // ============================================================

    @Operation(summary = "Список подгрупп", description = "Все подгруппы группы. **Доступ:** активные участники.")
    @ApiResponse(responseCode = "200", description = "Список подгрупп",
            content = @Content(schema = @Schema(implementation = ChargeSubgroupResponse.class)))
    @GetMapping("/{groupId}/subgroups")
    public ResponseEntity<List<ChargeSubgroupResponse>> listSubgroups(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @Parameter(description = "ID группы", example = "1") @PathVariable Long groupId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(groupService.listSubgroups(teamId, groupId, userId));
    }

    @Operation(summary = "Создать подгруппу", description = "**Доступ:** владелец/модератор.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Подгруппа создана",
                    content = @Content(schema = @Schema(implementation = ChargeSubgroupResponse.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "409", description = "Подгруппа с таким названием уже существует")
    })
    @PostMapping("/{groupId}/subgroups")
    public ResponseEntity<ChargeSubgroupResponse> createSubgroup(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @Parameter(description = "ID группы", example = "1") @PathVariable Long groupId,
            @Valid @RequestBody ChargeSubgroupRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(groupService.createSubgroup(teamId, groupId, userId, req));
    }

    @Operation(summary = "Переименовать подгруппу", description = "**Доступ:** владелец/модератор.")
    @ApiResponse(responseCode = "200", description = "Подгруппа переименована",
            content = @Content(schema = @Schema(implementation = ChargeSubgroupResponse.class)))
    @PutMapping("/subgroups/{subgroupId}")
    public ResponseEntity<ChargeSubgroupResponse> renameSubgroup(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @Parameter(description = "ID подгруппы", example = "1") @PathVariable Long subgroupId,
            @Valid @RequestBody ChargeSubgroupRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(groupService.renameSubgroup(teamId, subgroupId, userId, req));
    }

    @Operation(summary = "Удалить подгруппу", description = "**Доступ:** владелец/модератор.")
    @ApiResponse(responseCode = "200", description = "Подгруппа удалена")
    @DeleteMapping("/subgroups/{subgroupId}")
    public ResponseEntity<Map<String, String>> deleteSubgroup(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @Parameter(description = "ID подгруппы", example = "1") @PathVariable Long subgroupId) {
        Long userId = SecurityUtils.getCurrentUserId();
        groupService.deleteSubgroup(teamId, subgroupId, userId);
        return ResponseEntity.ok(Map.of("message", "Подгруппа удалена"));
    }
}