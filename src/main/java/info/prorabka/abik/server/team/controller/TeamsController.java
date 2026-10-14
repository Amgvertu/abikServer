package info.prorabka.abik.server.team.controller;

import info.prorabka.abik.server.common.security.SecurityUtils;
import info.prorabka.abik.server.team.dto.*;
import info.prorabka.abik.server.team.service.TeamInvitationService;
import info.prorabka.abik.server.team.service.TeamMemberService;
import info.prorabka.abik.server.team.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST-контроллер команд.
 * Все эндпоинты требуют аутентификации (см. SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/teams")
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

    /** Мои команды (активное членство). */
    @GetMapping("/my")
    public ResponseEntity<List<TeamResponse>> myTeams() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(teamService.listMyTeams(userId));
    }

    /** Создать команду. Создатель становится владельцем. */
    @PostMapping
    public ResponseEntity<TeamDetailResponse> create(
            @Valid @RequestBody TeamCreateRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(teamService.create(userId, req));
    }

    /** Детали команды. Доступно активным участникам. */
    @GetMapping("/{teamId}")
    public ResponseEntity<TeamDetailResponse> detail(@PathVariable Long teamId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(teamService.getDetail(teamId, userId));
    }

    /** Обновить команду. Владелец/модератор. */
    @PutMapping("/{teamId}")
    public ResponseEntity<TeamDetailResponse> update(
            @PathVariable Long teamId,
            @Valid @RequestBody TeamUpdateRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(teamService.update(teamId, userId, req));
    }

    /** Архивировать команду. Только владелец. */
    @PostMapping("/{teamId}/archive")
    public ResponseEntity<TeamDetailResponse> archive(@PathVariable Long teamId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(teamService.archive(teamId, userId));
    }

    // ============================================================
    // Передача владения
    // ============================================================

    /** Передать владение другому участнику. Только владелец. Необратимо. */
    @PostMapping("/{teamId}/transfer-ownership")
    public ResponseEntity<TeamDetailResponse> transferOwnership(
            @PathVariable Long teamId,
            @Valid @RequestBody TransferOwnershipRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(teamService.transferOwnership(teamId, userId, req.newOwnerUserId()));
    }

    // ============================================================
    // Участники
    // ============================================================

    /** Список активных участников. Доступно активным участникам. */
    @GetMapping("/{teamId}/members")
    public ResponseEntity<List<TeamMemberResponse>> members(@PathVariable Long teamId) {
        Long userId = SecurityUtils.getCurrentUserId();
        teamMemberService.requireMember(teamId, userId);
        return ResponseEntity.ok(teamMemberService.listActiveMembers(teamId));
    }

    /** Изменить роль участника (MEMBER ↔ MODERATOR). Только владелец. */
    @PutMapping("/{teamId}/members/{targetUserId}/role")
    public ResponseEntity<TeamMemberResponse> changeRole(
            @PathVariable Long teamId,
            @PathVariable Long targetUserId,
            @Valid @RequestBody UpdateMemberRoleRequest req) {
        Long actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(
                teamMemberService.changeRole(teamId, targetUserId, actorId, req));
    }

    /** Установить/снять игровой номер. Владелец/модератор. */
    @PutMapping("/{teamId}/members/{targetUserId}/jersey")
    public ResponseEntity<TeamMemberResponse> setJersey(
            @PathVariable Long teamId,
            @PathVariable Long targetUserId,
            @Valid @RequestBody SetJerseyNumberRequest req) {
        Long actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(
                teamMemberService.setJerseyNumber(teamId, targetUserId, actorId, req));
    }

    /** Исключить участника. Владелец/модератор. */
    @DeleteMapping("/{teamId}/members/{targetUserId}")
    public ResponseEntity<Map<String, String>> excludeMember(
            @PathVariable Long teamId,
            @PathVariable Long targetUserId) {
        Long actorId = SecurityUtils.getCurrentUserId();
        teamMemberService.excludeMember(teamId, targetUserId, actorId);
        return ResponseEntity.ok(Map.of("message", "Участник исключён"));
    }

    /** Восстановить участника (EXCLUDED/LEFT → ACTIVE). Владелец/модератор. */
    @PostMapping("/{teamId}/members/{targetUserId}/restore")
    public ResponseEntity<TeamMemberResponse> restoreMember(
            @PathVariable Long teamId,
            @PathVariable Long targetUserId) {
        Long actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(
                teamMemberService.restoreMember(teamId, targetUserId, actorId));
    }

    /** Выйти из команды. Владелец выйти не может. */
    @PostMapping("/{teamId}/leave")
    public ResponseEntity<Map<String, String>> leave(@PathVariable Long teamId) {
        Long userId = SecurityUtils.getCurrentUserId();
        teamMemberService.leaveTeam(teamId, userId);
        return ResponseEntity.ok(Map.of("message", "Вы покинули команду"));
    }

    // ============================================================
    // Приглашения в команде
    // ============================================================

    /** Отправить приглашение. Владелец/модератор. */
    @PostMapping("/{teamId}/invitations")
    public ResponseEntity<InvitationResponse> invite(
            @PathVariable Long teamId,
            @Valid @RequestBody InviteRequest req) {
        Long actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(
                teamInvitationService.invite(teamId, actorId, req.phone()));
    }

    /** Исходящие приглашения команды. Владелец/модератор. */
    @GetMapping("/{teamId}/invitations")
    public ResponseEntity<List<InvitationResponse>> outgoing(@PathVariable Long teamId) {
        Long actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(
                teamInvitationService.listTeamOutgoing(teamId, actorId));
    }
}