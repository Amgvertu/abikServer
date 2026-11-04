package info.prorabka.abik.server.release.controller;

import info.prorabka.abik.server.common.exception.ForbiddenException;
import info.prorabka.abik.server.common.exception.NotFoundException;
import info.prorabka.abik.server.common.security.SecurityUtils;
import info.prorabka.abik.server.release.dto.AppReleaseRequest;
import info.prorabka.abik.server.release.entity.AppRelease;
import info.prorabka.abik.server.release.repository.AppReleaseRepository;
import info.prorabka.abik.server.team.service.TeamMemberService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
public class AppReleaseController {
    private final AppReleaseRepository repository;
    private final TeamMemberService members;
    public AppReleaseController(AppReleaseRepository repository, TeamMemberService members) {
        this.repository = repository; this.members = members;
    }
    @GetMapping("/v1/app/releases/latest")
    public AppRelease latest(@RequestParam(defaultValue = "android") String platform) {
        return repository.findFirstByPlatformOrderByVersionCodeDesc(platform.toLowerCase())
                .orElseThrow(() -> new NotFoundException("Опубликованных версий пока нет"));
    }
    @PostMapping("/v1/admin/app-releases")
    @Transactional public AppRelease publish(@Valid @RequestBody AppReleaseRequest req) {
        if (!members.isSuperAdmin(SecurityUtils.getCurrentUserId())) {
            throw new ForbiddenException("Доступно только суперадмину");
        }
        AppRelease release = repository.findByPlatformAndVersionCode("android", req.versionCode())
                .orElseGet(() -> AppRelease.builder().platform("android")
                        .versionCode(req.versionCode()).build());
        release.setVersionName(req.versionName().trim()); release.setReleaseNotes(req.releaseNotes().trim());
        release.setDownloadUrl(req.downloadUrl().trim()); release.setMandatory(req.mandatory());
        return repository.save(release);
    }
}
