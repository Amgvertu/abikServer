package info.prorabka.abik.server.charge.controller;

import info.prorabka.abik.server.charge.dto.ActiveSubscriptionResponse;
import info.prorabka.abik.server.charge.service.SubscriptionPollService;
import info.prorabka.abik.server.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/subscription-polls")
@Tag(name = "Subscription Polls", description = "Действующие абонементы пользователя")
@SecurityRequirement(name = "bearerAuth")
public class UserSubscriptionPollsController {
    private final SubscriptionPollService pollService;

    public UserSubscriptionPollsController(SubscriptionPollService pollService) {
        this.pollService = pollService;
    }

    @GetMapping("/active")
    public List<ActiveSubscriptionResponse> active() {
        return pollService.activeSubscriptions(SecurityUtils.getCurrentUserId());
    }
}
