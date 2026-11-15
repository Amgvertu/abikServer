package info.prorabka.abik.server.admin.controller;

import info.prorabka.abik.server.admin.dto.*;
import info.prorabka.abik.server.admin.dto.AdminDtos.*;
import info.prorabka.abik.server.admin.service.*;
import info.prorabka.abik.server.common.security.SecurityUtils;
import info.prorabka.abik.server.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/admin")
public class AdminOperationsController {
    private final AdminAccessService access;
    private final AdminOperationsService operations;
    private final AdminMessageService messages;
    private final AdvertisingService ads;
    private final RegistrationLogService registrations;

    public AdminOperationsController(AdminAccessService access,AdminOperationsService operations,AdminMessageService messages,AdvertisingService ads,RegistrationLogService registrations){
        this.access=access;this.operations=operations;this.messages=messages;this.ads=ads;this.registrations=registrations;
    }
    private Long superAdmin(){Long id=SecurityUtils.getCurrentUserId();access.requireSuperAdmin(id);return id;}
    private User adManager(){return access.requireAdManager(SecurityUtils.getCurrentUserId());}

    @GetMapping("/overview") public Overview overview(@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate from,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate to,@RequestParam(required=false)Long regionId,@RequestParam(required=false)Long cityId,@RequestParam(required=false)Long teamId){superAdmin();return operations.overview(from,to,regionId,cityId,teamId);}
    @GetMapping("/manage/users") public List<UserRow> users(@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate from,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate to,@RequestParam(required=false)Long regionId,@RequestParam(required=false)Long cityId,@RequestParam(required=false)Long teamId){superAdmin();return operations.listUsers(from,to,regionId,cityId,teamId);}
    @GetMapping("/manage/users/{id}") public UserDetails user(@PathVariable Long id){superAdmin();return operations.userDetails(id);}
    @PostMapping("/manage/users/{id}/block") public UserRow blockUser(@PathVariable Long id,@Valid @RequestBody BlockRequest request){return operations.blockUser(id,superAdmin(),request.reason());}
    @PostMapping("/manage/users/{id}/unblock") public UserRow unblockUser(@PathVariable Long id){superAdmin();return operations.unblockUser(id);}
    @PostMapping("/manage/users/{id}/ad-admin") public UserRow adAdmin(@PathVariable Long id,@RequestBody Map<String,Boolean> body){superAdmin();return operations.setAdAdmin(id,Boolean.TRUE.equals(body.get("value")));}
    @GetMapping("/manage/teams") public List<TeamRow> teams(@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate from,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate to,@RequestParam(required=false)Long regionId,@RequestParam(required=false)Long cityId,@RequestParam(required=false)Long teamId){superAdmin();return operations.listTeams(from,to,regionId,cityId,teamId);}
    @GetMapping("/manage/teams/{id}") public TeamDetails team(@PathVariable Long id){superAdmin();return operations.teamDetails(id);}
    @PostMapping("/manage/teams/{id}/block") public TeamRow blockTeam(@PathVariable Long id,@Valid @RequestBody BlockRequest request){superAdmin();return operations.blockTeam(id,request.reason(),SecurityUtils.getCurrentUserId());}
    @PostMapping("/manage/teams/{id}/unblock") public TeamRow unblockTeam(@PathVariable Long id){superAdmin();return operations.unblockTeam(id);}

    @PostMapping("/messages") public MessageRow send(@Valid @RequestBody AdminMessageRequest request){return messages.send(superAdmin(),request);}
    @PostMapping("/messages/recipients") public Map<String,Long> recipients(@RequestBody AdminMessageRequest request){superAdmin();return Map.of("count",messages.countRecipients(request));}
    @GetMapping("/messages") public List<MessageRow> messages(){superAdmin();return messages.history();}
    @GetMapping("/registration-events") public List<RegistrationEventRow> registrationEvents(@RequestParam(required=false)String status,@RequestParam(required=false)String stage,@RequestParam(defaultValue="200")int limit){superAdmin();return registrations.list(status,stage,limit);}

    @GetMapping("/ads") public List<AdvertisementRow> ads(){return ads.list(adManager());}
    @GetMapping("/ad-targets/teams") public List<TargetOption> adTargetTeams(){adManager();return operations.adTargetTeams();}
    @GetMapping("/ads/{id}") public AdvertisementRow ad(@PathVariable Long id){return ads.get(adManager(),id);}
    @PostMapping("/ads") public AdvertisementRow createAd(@Valid @RequestBody AdvertisementRequest request){return ads.create(adManager(),request);}
    @PutMapping("/ads/{id}") public AdvertisementRow updateAd(@PathVariable Long id,@Valid @RequestBody AdvertisementRequest request){return ads.update(adManager(),id,request);}
    @PostMapping("/ads/{id}/active") public AdvertisementRow activeAd(@PathVariable Long id,@RequestBody Map<String,Boolean> body){return ads.setActive(adManager(),id,Boolean.TRUE.equals(body.get("value")));}
    @DeleteMapping("/ads/{id}") public ResponseEntity<Void> deleteAd(@PathVariable Long id){ads.delete(adManager(),id);return ResponseEntity.noContent().build();}
    @GetMapping("/ads/{id}/stats") public AdStats adStats(@PathVariable Long id,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate from,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate to,@RequestParam(required=false)Long regionId,@RequestParam(required=false)Long cityId,@RequestParam(required=false)Long teamId){return ads.stats(adManager(),id,from,to,regionId,cityId,teamId);}
}
