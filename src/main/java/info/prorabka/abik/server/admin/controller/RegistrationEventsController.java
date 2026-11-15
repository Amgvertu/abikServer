package info.prorabka.abik.server.admin.controller;

import info.prorabka.abik.server.admin.dto.RegistrationClientEventRequest;
import info.prorabka.abik.server.admin.service.RegistrationLogService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Технические ошибки клиента до входа в аккаунт. Пароли и SMS-коды не принимаются. */
@RestController
@RequestMapping("/v1/auth/registration-events")
public class RegistrationEventsController {
    private final RegistrationLogService logs;
    public RegistrationEventsController(RegistrationLogService logs){this.logs=logs;}
    @PostMapping public ResponseEntity<Void> create(@Valid @RequestBody RegistrationClientEventRequest request){logs.recordClient(request);return ResponseEntity.accepted().build();}
}
