package info.prorabka.abik.server.admin.controller;

import info.prorabka.abik.server.admin.dto.AdminDtos.AdvertisementRow;
import info.prorabka.abik.server.admin.dto.AdvertisementEventRequest;
import info.prorabka.abik.server.admin.service.AdvertisingService;
import info.prorabka.abik.server.common.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/ads")
public class AdvertisementsController {
    private final AdvertisingService ads;
    public AdvertisementsController(AdvertisingService ads){this.ads=ads;}
    @GetMapping("/active") public ResponseEntity<AdvertisementRow> active(@RequestParam(required=false)Long teamId){AdvertisementRow ad=ads.activeForUser(SecurityUtils.getCurrentUserId(),teamId);return ad==null?ResponseEntity.noContent().build():ResponseEntity.ok(ad);}
    @PostMapping("/{id}/events") public ResponseEntity<Void> event(@PathVariable Long id,@Valid @RequestBody AdvertisementEventRequest request){ads.recordEvent(SecurityUtils.getCurrentUserId(),id,request);return ResponseEntity.accepted().build();}
}
