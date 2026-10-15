package info.prorabka.abik.server.team.controller;

import info.prorabka.abik.server.common.security.SecurityUtils;
import info.prorabka.abik.server.team.dto.InvitationResponse;
import info.prorabka.abik.server.team.service.TeamInvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST-контроллер приглашений — действия со стороны приглашённого и отправителя.
 */
@RestController
@RequestMapping("/v1/invitations")
@Tag(name = "Invitations",
        description = """
             Работа с приглашениями в команды.

             **Входящие приглашения** — те, что отправлены текущему пользователю.
             **Действия** — принять/отклонить их может только приглашённый.
             **Отмена** — доступна отправителю (владельцу/модератору команды).
             """)
@SecurityRequirement(name = "bearerAuth")
public class InvitationsController {

    private final TeamInvitationService invitationService;

    public InvitationsController(TeamInvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @Operation(
            summary = "Мои входящие приглашения",
            description = "PENDING-приглашения, адресованные текущему пользователю. Просроченные автоматически помечаются EXPIRED."
    )
    @ApiResponse(responseCode = "200", description = "Список приглашений",
            content = @Content(schema = @Schema(implementation = InvitationResponse.class)))
    @GetMapping("/my")
    public ResponseEntity<List<InvitationResponse>> myIncoming() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(invitationService.listMyIncoming(userId));
    }

    @Operation(
            summary = "Принять приглашение",
            description = """
                    Принимает приглашение, добавляет пользователя в команду с ролью MEMBER.
                    **Доступ:** только приглашённый.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Приглашение принято",
                    content = @Content(schema = @Schema(implementation = InvitationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Приглашение просрочено или уже обработано"),
            @ApiResponse(responseCode = "403", description = "Приглашение адресовано другому пользователю"),
            @ApiResponse(responseCode = "404", description = "Приглашение не найдено"),
            @ApiResponse(responseCode = "409", description = "Вы уже являетесь участником команды")
    })
    @PostMapping("/{invitationId}/accept")
    public ResponseEntity<InvitationResponse> accept(
            @Parameter(description = "ID приглашения", example = "1")
            @PathVariable Long invitationId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(invitationService.accept(invitationId, userId));
    }

    @Operation(
            summary = "Отклонить приглашение",
            description = "Помечает приглашение статусом DECLINED. **Доступ:** только приглашённый."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Приглашение отклонено",
                    content = @Content(schema = @Schema(implementation = InvitationResponse.class))),
            @ApiResponse(responseCode = "403", description = "Приглашение адресовано другому пользователю"),
            @ApiResponse(responseCode = "404", description = "Приглашение не найдено")
    })
    @PostMapping("/{invitationId}/decline")
    public ResponseEntity<InvitationResponse> decline(
            @Parameter(description = "ID приглашения", example = "1")
            @PathVariable Long invitationId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(invitationService.decline(invitationId, userId));
    }

    @Operation(
            summary = "Отменить приглашение",
            description = """
                    Помечает приглашение статусом CANCELLED.
                    **Доступ:** владелец или модератор команды, из которой отправлено приглашение.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Приглашение отменено",
                    content = @Content(schema = @Schema(implementation = InvitationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Приглашение уже обработано"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав в команде"),
            @ApiResponse(responseCode = "404", description = "Приглашение не найдено")
    })
    @PostMapping("/{invitationId}/cancel")
    public ResponseEntity<InvitationResponse> cancel(
            @Parameter(description = "ID приглашения", example = "1")
            @PathVariable Long invitationId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(invitationService.cancel(invitationId, userId));
    }
}