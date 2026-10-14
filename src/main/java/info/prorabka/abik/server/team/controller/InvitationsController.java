package info.prorabka.abik.server.team.controller;

import info.prorabka.abik.server.common.security.SecurityUtils;
import info.prorabka.abik.server.team.dto.InvitationResponse;
import info.prorabka.abik.server.team.service.TeamInvitationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST-контроллер приглашений (действия со стороны приглашённого и отправителя).
 */
@RestController
@RequestMapping("/api/v1/invitations")
public class InvitationsController {

    private final TeamInvitationService invitationService;

    public InvitationsController(TeamInvitationService invitationService) {
        this.invitationService = invitationService;
    }

    /** Мои входящие приглашения (PENDING). */
    @GetMapping("/my")
    public ResponseEntity<List<InvitationResponse>> myIncoming() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(invitationService.listMyIncoming(userId));
    }

    /** Принять приглашение. */
    @PostMapping("/{invitationId}/accept")
    public ResponseEntity<InvitationResponse> accept(@PathVariable Long invitationId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(invitationService.accept(invitationId, userId));
    }

    /** Отклонить приглашение. */
    @PostMapping("/{invitationId}/decline")
    public ResponseEntity<InvitationResponse> decline(@PathVariable Long invitationId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(invitationService.decline(invitationId, userId));
    }

    /** Отменить приглашение (отправителем). Владелец/модератор команды. */
    @PostMapping("/{invitationId}/cancel")
    public ResponseEntity<InvitationResponse> cancel(@PathVariable Long invitationId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(invitationService.cancel(invitationId, userId));
    }
}