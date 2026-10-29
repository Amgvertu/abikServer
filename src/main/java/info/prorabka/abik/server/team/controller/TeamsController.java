package info.prorabka.abik.server.team.controller;

import info.prorabka.abik.server.common.security.SecurityUtils;
import info.prorabka.abik.server.team.dto.*;
import info.prorabka.abik.server.team.service.TeamInvitationService;
import info.prorabka.abik.server.user.dto.UserProfileRequest;
import info.prorabka.abik.server.user.dto.UserProfileResponse;
import info.prorabka.abik.server.team.service.TeamMemberService;
import info.prorabka.abik.server.team.service.TeamService;
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
 * REST-контроллер команд.
 * Все эндпоинты требуют аутентификации.
 */
@RestController
@RequestMapping("/v1/teams")
@Tag(name = "Teams",
        description = """
             Управление командами: создание, участники, роли, игровые номера,
             передача владения, архивация, приглашения.

             **Роли:**
             - **OWNER** (владелец) — один на команду, может всё.
             - **MODERATOR** — управляет участниками и начислениями, но не может передать владение и архивировать.
             - **MEMBER** — обычный участник.
             """)
@SecurityRequirement(name = "bearerAuth")
public class TeamsController {
    private final TeamService teamService;
    private final TeamMemberService teamMemberService;
    private final TeamInvitationService teamInvitationService;

    public TeamsController(TeamService teamService,
                           TeamMemberService teamMemberService,
                           TeamInvitationService teamInvitationService) {
        this.teamService = teamService;
        this.teamMemberService = teamMemberService;
        this.teamInvitationService = teamInvitationService;
    }

    // ============================================================
    // Команды — CRUD
    // ============================================================

    @Operation(
            summary = "Мои команды",
            description = "Список команд, в которых текущий пользователь является активным участником."
    )
    @ApiResponse(responseCode = "200", description = "Список команд",
            content = @Content(schema = @Schema(implementation = TeamResponse.class)))
    @GetMapping("/my")
    public ResponseEntity<List<TeamResponse>> myTeams() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(teamService.listMyTeams(userId));
    }

    @Operation(
            summary = "Создать команду",
            description = """
                    Создаёт новую команду. Создатель **автоматически становится владельцем** (OWNER).

                    **Обязательные поля:** name, cityId, homeRegionId, logoUrl.
                    **Опциональные:** description, season.

                    **Доступ:** любой аутентифицированный пользователь.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Команда создана",
                    content = @Content(schema = @Schema(implementation = TeamDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "404", description = "Указанный город или регион не найден")
    })
    @PostMapping
    public ResponseEntity<TeamDetailResponse> create(
            @Valid @RequestBody TeamCreateRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(teamService.create(userId, req));
    }

    @Operation(
            summary = "Детали команды",
            description = "Полная информация о команде + роль текущего пользователя в ней. **Доступ:** активные участники."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Детали команды",
                    content = @Content(schema = @Schema(implementation = TeamDetailResponse.class))),
            @ApiResponse(responseCode = "403", description = "Пользователь не является участником"),
            @ApiResponse(responseCode = "404", description = "Команда не найдена")
    })
    @GetMapping("/{teamId}")
    public ResponseEntity<TeamDetailResponse> detail(
            @Parameter(description = "ID команды", example = "1")
            @PathVariable Long teamId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(teamService.getDetail(teamId, userId));
    }

    @Operation(
            summary = "Обновить команду",
            description = "Изменение основных полей команды. **Доступ:** владелец или модератор. Архивную команду редактировать нельзя."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Команда обновлена",
                    content = @Content(schema = @Schema(implementation = TeamDetailResponse.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав или команда в архиве"),
            @ApiResponse(responseCode = "404", description = "Команда, город или регион не найдены")
    })
    @PutMapping("/{teamId}")
    public ResponseEntity<TeamDetailResponse> update(
            @Parameter(description = "ID команды", example = "1")
            @PathVariable Long teamId,
            @Valid @RequestBody TeamUpdateRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(teamService.update(teamId, userId, req));
    }

    @Operation(
            summary = "Архивировать команду",
            description = """
                    Переводит команду в статус ARCHIVED. Данные сохраняются, начисления останавливаются.
                    **Доступ:** только владелец.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Команда архивирована",
                    content = @Content(schema = @Schema(implementation = TeamDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "Команда уже в архиве"),
            @ApiResponse(responseCode = "403", description = "Только владелец может архивировать")
    })
    @PostMapping("/{teamId}/archive")
    public ResponseEntity<TeamDetailResponse> archive(
            @Parameter(description = "ID команды", example = "1")
            @PathVariable Long teamId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(teamService.archive(teamId, userId));
    }

    // ============================================================
    // Передача владения
    // ============================================================

    @Operation(
            summary = "Передать владение командой",
            description = """
                    **Необратимая операция.** Бывший владелец становится модератором, новый — владельцем.

                    **Условия:** новый владелец должен быть активным участником.
                    **Доступ:** только текущий владелец.
                    **История:** каждая передача записывается в ownership_transfers.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Владение передано",
                    content = @Content(schema = @Schema(implementation = TeamDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "Уже владелец / новый владелец не участник"),
            @ApiResponse(responseCode = "403", description = "Только владелец может передать владение")
    })
    @PostMapping("/{teamId}/transfer-ownership")
    public ResponseEntity<TeamDetailResponse> transferOwnership(
            @Parameter(description = "ID команды", example = "1")
            @PathVariable Long teamId,
            @Valid @RequestBody TransferOwnershipRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(teamService.transferOwnership(teamId, userId, req.newOwnerUserId()));
    }

    // ============================================================
    // Участники
    // ============================================================

    @Operation(summary = "Карточка участника",
            description = """
                    Team-scoped карточка участника (ТЗ 9.2): телефон, роль, статус членства,
                    игровой номер, признак тренера и профиль (ФИО, аватарка, амплуа,
                    регион/город, рост/вес, хват, размеры экипировки).

                    **Доступ:** активные участники той же команды.
                    Если профиль не заполнен — поле `profile` равно null.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Карточка участника",
                    content = @Content(schema = @Schema(implementation = MemberDetailsResponse.class))),
            @ApiResponse(responseCode = "403", description = "Вы не участник этой команды"),
            @ApiResponse(responseCode = "404", description = "Участник не найден в команде")
    })
    @GetMapping("/{teamId}/members/{targetUserId}")
    public ResponseEntity<MemberDetailsResponse> memberDetails(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @Parameter(description = "ID участника", example = "1") @PathVariable Long targetUserId) {
        Long actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(teamMemberService.getMemberDetails(teamId, targetUserId, actor));
    }


    @Operation(
            summary = "Список участников",
            description = "Активные участники команды. **Доступ:** активные участники."
    )
    @ApiResponse(responseCode = "200", description = "Список участников",
            content = @Content(schema = @Schema(implementation = TeamMemberResponse.class)))
    @GetMapping("/{teamId}/members")
    public ResponseEntity<List<TeamMemberResponse>> members(
            @Parameter(description = "ID команды", example = "1")
            @PathVariable Long teamId) {
        Long userId = SecurityUtils.getCurrentUserId();
        teamMemberService.requireMember(teamId, userId);
        return ResponseEntity.ok(teamMemberService.listActiveMembers(teamId));
    }

    @Operation(
            summary = "Изменить роль участника",
            description = """
                    Назначает или снимает роль MODERATOR. Через этот метод **нельзя** назначить OWNER —
                    для этого есть отдельный endpoint `transfer-ownership`.

                    **Доступ:** только владелец.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Роль изменена",
                    content = @Content(schema = @Schema(implementation = TeamMemberResponse.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав или попытка назначить OWNER")
    })
    @PutMapping("/{teamId}/members/{targetUserId}/role")
    public ResponseEntity<TeamMemberResponse> changeRole(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @Parameter(description = "ID участника", example = "2") @PathVariable Long targetUserId,
            @Valid @RequestBody UpdateMemberRoleRequest req) {
        Long actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(
                teamMemberService.changeRole(teamId, targetUserId, actorId, req));
    }

    @Operation(
            summary = "Установить игровой номер",
            description = """
                    Задаёт или снимает игровой номер участника.
                    Номер **уникален в команде** среди активных участников.

                    **Доступ:** владелец или модератор. Передать `null` — снять номер.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Номер установлен",
                    content = @Content(schema = @Schema(implementation = TeamMemberResponse.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав или номер занят"),
            @ApiResponse(responseCode = "404", description = "Участник не найден в команде")
    })
    @PutMapping("/{teamId}/members/{targetUserId}/jersey")
    public ResponseEntity<TeamMemberResponse> setJersey(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @Parameter(description = "ID участника", example = "2") @PathVariable Long targetUserId,
            @Valid @RequestBody SetJerseyNumberRequest req) {
        Long actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(
                teamMemberService.setJerseyNumber(teamId, targetUserId, actorId, req));
    }

    @Operation(
            summary = "Исключить участника",
            description = """
                    Переводит участника в статус EXCLUDED. Долги сохраняются, вернуть можно через `restore`.

                    **Доступ:** владелец или модератор.
                    **Ограничения:** модератор не может исключить владельца или другого модератора;
                    владельца нельзя исключить вообще.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Участник исключён"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    @DeleteMapping("/{teamId}/members/{targetUserId}")
    public ResponseEntity<Map<String, String>> excludeMember(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @Parameter(description = "ID участника", example = "2") @PathVariable Long targetUserId) {
        Long actorId = SecurityUtils.getCurrentUserId();
        teamMemberService.excludeMember(teamId, targetUserId, actorId);
        return ResponseEntity.ok(Map.of("message", "Участник исключён"));
    }

    @Operation(
            summary = "Восстановить участника",
            description = """
                    Возвращает участника из статуса EXCLUDED/LEFT в ACTIVE.
                    **Доступ:** владелец или модератор.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Участник восстановлен",
            content = @Content(schema = @Schema(implementation = TeamMemberResponse.class)))
    @PostMapping("/{teamId}/members/{targetUserId}/restore")
    public ResponseEntity<TeamMemberResponse> restoreMember(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @Parameter(description = "ID участника", example = "2") @PathVariable Long targetUserId) {
        Long actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(
                teamMemberService.restoreMember(teamId, targetUserId, actorId));
    }

    @Operation(
            summary = "Покинуть команду",
            description = """
                    Текущий пользователь выходит из команды. Статус становится LEFT, номер освобождается.
                    **Владелец не может выйти** — сначала передайте владение или архивируйте команду.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Вы вышли из команды"),
            @ApiResponse(responseCode = "403", description = "Владелец не может покинуть команду")
    })
    @PostMapping("/{teamId}/leave")
    public ResponseEntity<Map<String, String>> leave(
            @Parameter(description = "ID команды", example = "1")
            @PathVariable Long teamId) {
        Long userId = SecurityUtils.getCurrentUserId();
        teamMemberService.leaveTeam(teamId, userId);
        return ResponseEntity.ok(Map.of("message", "Вы покинули команду"));
    }

    // ============================================================
    // Приглашения (часть внутри команды)
    // ============================================================

    @Operation(
            summary = "Отправить приглашение",
            description = """
                    Приглашает пользователя в команду по номеру телефона.
                    **Только зарегистрированным** в системе пользователям.

                    **Доступ:** владелец или модератор.
                    **Срок действия:** 7 дней.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Приглашение создано",
                    content = @Content(schema = @Schema(implementation = InvitationResponse.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав или команда в архиве"),
            @ApiResponse(responseCode = "404", description = "Пользователь с таким номером не зарегистрирован"),
            @ApiResponse(responseCode = "409", description = "Приглашение уже отправлено или пользователь уже в команде")
    })
    @PostMapping("/{teamId}/invitations")
    public ResponseEntity<InvitationResponse> invite(
            @Parameter(description = "ID команды", example = "1")
            @PathVariable Long teamId,
            @Valid @RequestBody InviteRequest req) {
        Long actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(
                teamInvitationService.invite(teamId, actorId, req.phone()));
    }

    @Operation(
            summary = "Исходящие приглашения команды",
            description = "PENDING-приглашения, отправленные из команды. **Доступ:** владелец или модератор."
    )
    @ApiResponse(responseCode = "200", description = "Список приглашений",
            content = @Content(schema = @Schema(implementation = InvitationResponse.class)))
    @GetMapping("/{teamId}/invitations")
    public ResponseEntity<List<InvitationResponse>> outgoing(
            @Parameter(description = "ID команды", example = "1")
            @PathVariable Long teamId) {
        Long actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(
                teamInvitationService.listTeamOutgoing(teamId, actorId));
    }

    @Operation(summary = "Назначить/снять тренера",
            description = "Доступ: владелец или админ команды.")
    @PutMapping("/{teamId}/members/{targetUserId}/trainer")
    public ResponseEntity<TeamMemberResponse> setTrainer(
            @PathVariable Long teamId,
            @PathVariable Long targetUserId,
            @RequestBody Map<String, Boolean> body) {
        Long actorId = SecurityUtils.getCurrentUserId();
        boolean flag = Boolean.TRUE.equals(body.get("isTrainer"));
        return ResponseEntity.ok(
                teamMemberService.setTrainer(teamId, targetUserId, actorId, flag));
    }

    @Operation(
            summary = "Редактировать профиль участника",
            description = """
                    Управляющий команды создаёт/обновляет профиль участника (BE-15b).
                    Тело и валидация — те же, что у PUT /v1/users/me/profile.

                    **Доступ:** владелец или модератор команды (или суперадмин).
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Профиль сохранён",
                    content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации полей профиля"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Участник не найден в команде")
    })
    @PutMapping("/{teamId}/members/{targetUserId}/profile")
    public ResponseEntity<UserProfileResponse> updateMemberProfile(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @Parameter(description = "ID участника", example = "1") @PathVariable Long targetUserId,
            @Valid @RequestBody UserProfileRequest req) {
        Long actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(
                teamMemberService.updateMemberProfile(teamId, targetUserId, actorId, req));
    }
}