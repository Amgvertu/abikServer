package info.prorabka.abik.server.admin.controller;

import info.prorabka.abik.server.admin.dto.AdminUserResponse;
import info.prorabka.abik.server.admin.dto.CreateCityRequest;
import info.prorabka.abik.server.admin.dto.CreateRegionRequest;
import info.prorabka.abik.server.admin.service.AdminService;
import info.prorabka.abik.server.common.security.SecurityUtils;
import info.prorabka.abik.server.geo.entity.City;
import info.prorabka.abik.server.geo.entity.Region;
import info.prorabka.abik.server.team.dto.TeamResponse;
import info.prorabka.abik.server.team.service.TeamMemberService;
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

/**
 * Панель суперадмина.
 */
@RestController
@RequestMapping("/v1/admin")
@Tag(name = "Admin", description = "Функции суперадмина. Доступно только пользователям с флагом isSuperAdmin.")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;
    private final TeamMemberService teamMemberService;

    public AdminController(AdminService adminService, TeamMemberService teamMemberService) {
        this.adminService = adminService;
        this.teamMemberService = teamMemberService;
    }

    // ============================================================
    // Проверка прав
    // ============================================================
    private void requireSuperAdmin() {
        Long actor = SecurityUtils.getCurrentUserId();
        if (!teamMemberService.isSuperAdmin(actor)) {
            throw new info.prorabka.abik.server.common.exception.ForbiddenException("Доступно только суперадмину");
        }
    }

    // ============================================================
    // Пользователи
    // ============================================================

    @Operation(summary = "Список всех пользователей")
    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> listUsers() {
        requireSuperAdmin();
        return ResponseEntity.ok(adminService.listUsers());
    }

    @Operation(summary = "Заблокировать пользователя")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Пользователь заблокирован",
                    content = @Content(schema = @Schema(implementation = AdminUserResponse.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
            @ApiResponse(responseCode = "409", description = "Нельзя заблокировать себя или уже заблокирован")
    })
    @PostMapping("/users/{userId}/block")
    public ResponseEntity<AdminUserResponse> blockUser(
            @Parameter(description = "ID пользователя", example = "2") @PathVariable Long userId) {
        requireSuperAdmin();
        Long actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(adminService.blockUser(userId, actor));
    }

    @Operation(summary = "Разблокировать пользователя")
    @ApiResponse(responseCode = "200", description = "Пользователь разблокирован",
            content = @Content(schema = @Schema(implementation = AdminUserResponse.class)))
    @PostMapping("/users/{userId}/unblock")
    public ResponseEntity<AdminUserResponse> unblockUser(
            @Parameter(description = "ID пользователя", example = "2") @PathVariable Long userId) {
        requireSuperAdmin();
        Long actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(adminService.unblockUser(userId, actor));
    }

    // ============================================================
    // Команды
    // ============================================================

    @Operation(summary = "Список всех команд")
    @ApiResponse(responseCode = "200", description = "Список команд",
            content = @Content(schema = @Schema(implementation = TeamResponse.class)))
    @GetMapping("/teams")
    public ResponseEntity<List<TeamResponse>> listTeams() {
        requireSuperAdmin();
        return ResponseEntity.ok(adminService.listAllTeams());
    }

    // ============================================================
    // Справочники
    // ============================================================

    @Operation(summary = "Создать регион")
    @PostMapping("/regions")
    public ResponseEntity<Region> createRegion(@Valid @RequestBody CreateRegionRequest req) {
        requireSuperAdmin();
        return ResponseEntity.ok(adminService.createRegion(req));
    }

    @Operation(summary = "Создать город")
    @PostMapping("/cities")
    public ResponseEntity<City> createCity(@Valid @RequestBody CreateCityRequest req) {
        requireSuperAdmin();
        return ResponseEntity.ok(adminService.createCity(req));
    }
}