package info.prorabka.abik.server.notification.controller;

import info.prorabka.abik.server.common.security.SecurityUtils;
import info.prorabka.abik.server.notification.dto.DeviceRegisterRequest;
import info.prorabka.abik.server.notification.entity.UserDevice;
import info.prorabka.abik.server.notification.repository.UserDeviceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/v1/devices")
@Tag(name = "Devices", description = "Устройства для отправки push")
@SecurityRequirement(name = "bearerAuth")
public class DevicesController {

    private final UserDeviceRepository repository;

    public DevicesController(UserDeviceRepository repository) {
        this.repository = repository;
    }

    @Operation(summary = "Зарегистрировать устройство")
    @PostMapping
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody DeviceRegisterRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();

        UserDevice device = repository.findByToken(req.token())
                .orElseGet(() -> UserDevice.builder()
                        .userId(userId)
                        .token(req.token())
                        .build());

        // Если токен уже был у другого пользователя — перепривязываем
        device.setUserId(userId);
        device.setPlatform(req.platform());
        device.setLastSeenAt(OffsetDateTime.now());
        repository.save(device);

        return ResponseEntity.ok(Map.of("message", "Устройство зарегистрировано"));
    }

    @Operation(summary = "Отозвать устройство")
    @DeleteMapping("/{token}")
    public ResponseEntity<Map<String, String>> unregister(@PathVariable String token) {
        Long userId = SecurityUtils.getCurrentUserId();
        repository.findByToken(token).ifPresent(d -> {
            if (d.getUserId().equals(userId)) {
                repository.delete(d);
            }
        });
        return ResponseEntity.ok(Map.of("message", "Устройство отозвано"));
    }
}