package info.prorabka.abik.server.charge.controller;

import info.prorabka.abik.server.charge.dto.SubscriptionPollResponse;
import info.prorabka.abik.server.charge.dto.SubscriptionPollVoteRequest;
import info.prorabka.abik.server.charge.service.SubscriptionPollService;
import info.prorabka.abik.server.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/teams/{teamId}/subscription-polls")
@Tag(name = "Subscription Polls", description = "Ежемесячный абонемент по опросу")
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionPollsController {
    private final SubscriptionPollService pollService;

    public SubscriptionPollsController(SubscriptionPollService pollService) {
        this.pollService = pollService;
    }

    @GetMapping("/current")
    public ResponseEntity<SubscriptionPollResponse> current(@PathVariable Long teamId) {
        SubscriptionPollResponse response = pollService.current(teamId, SecurityUtils.getCurrentUserId());
        return response == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(response);
    }

    @PostMapping("/{pollId}/vote")
    public ResponseEntity<SubscriptionPollResponse> vote(@PathVariable Long teamId,
                                                         @PathVariable Long pollId,
                                                         @Valid @RequestBody SubscriptionPollVoteRequest request) {
        return ResponseEntity.ok(pollService.vote(
                teamId, pollId, SecurityUtils.getCurrentUserId(), request));
    }
}
