package info.prorabka.abik.server.guide.controller;

import info.prorabka.abik.server.common.exception.BadRequestException;
import info.prorabka.abik.server.common.security.SecurityUtils;
import info.prorabka.abik.server.guide.entity.GuideProgress;
import info.prorabka.abik.server.guide.repository.GuideProgressRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/v1/guide/progress")
public class GuideController {
    private final GuideProgressRepository repository;
    public GuideController(GuideProgressRepository repository) { this.repository = repository; }
    @GetMapping public List<String> completed() {
        return repository.findAllByUserId(SecurityUtils.getCurrentUserId()).stream()
                .map(p -> p.getScreenKey() + ":" + p.getRoleKey()).toList();
    }
    @PostMapping("/{screenKey}/{roleKey}")
    @Transactional public void complete(@PathVariable String screenKey, @PathVariable String roleKey) {
        validate(screenKey); validate(roleKey);
        Long userId = SecurityUtils.getCurrentUserId();
        if (repository.findByUserIdAndScreenKeyAndRoleKey(userId, screenKey, roleKey).isEmpty()) {
            repository.save(GuideProgress.builder().userId(userId)
                    .screenKey(screenKey).roleKey(roleKey).build());
        }
    }
    private static void validate(String value) {
        if (!value.matches("[a-zA-Z0-9_-]{1,80}")) throw new BadRequestException("Некорректный ключ обучения");
    }
}
