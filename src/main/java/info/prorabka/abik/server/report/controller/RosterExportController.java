package info.prorabka.abik.server.report.controller;

import info.prorabka.abik.server.common.security.SecurityUtils;
import info.prorabka.abik.server.report.service.RosterExportService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/v1/teams/{teamId}/reports")
public class RosterExportController {
    private final RosterExportService service;
    public RosterExportController(RosterExportService service) { this.service = service; }
    @GetMapping("/roster/export")
    public ResponseEntity<byte[]> export(@PathVariable Long teamId, @RequestParam(required = false) Long listId) {
        byte[] body = service.export(teamId, listId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"roster-team-" + teamId + "-" + LocalDate.now() + ".xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(body.length).body(body);
    }
}
