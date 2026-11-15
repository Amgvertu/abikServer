package info.prorabka.abik.server.admin.service;

import info.prorabka.abik.server.admin.dto.AdminDtos.RegistrationEventRow;
import info.prorabka.abik.server.admin.dto.RegistrationClientEventRequest;
import info.prorabka.abik.server.notification.entity.NotificationType;
import info.prorabka.abik.server.notification.service.NotificationService;
import info.prorabka.abik.server.user.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RegistrationLogService {
    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final NotificationService notifications;

    public RegistrationLogService(JdbcTemplate jdbc, UserRepository users, NotificationService notifications) {
        this.jdbc=jdbc; this.users=users; this.notifications=notifications;
    }

    public void record(String requestId, String phone, String stage, String status, String code, String details, String client) {
        try {
            jdbc.update("INSERT INTO registration_events(request_id,phone_masked,stage,status,code,details,client) VALUES (?,?,?,?,?,?,?)",
                    safe(requestId,100),maskPhone(phone),safe(stage,50),safe(status,30),safe(code,80),sanitize(details),safe(client,30));
        } catch (Exception ignored) { /* Журнал не должен ломать регистрацию. */ }
    }

    public void recordClient(RegistrationClientEventRequest request) {
        if (!List.of("ANDROID","WEB","IOS").contains(request.client().toUpperCase())) {
            record(request.requestId(),request.phone(),request.stage(),request.status(),request.code(),request.details(),request.client());
            return;
        }
        record(request.requestId(),request.phone(),request.stage(),request.status(),request.code(),request.details(),request.client().toUpperCase());
    }

    public List<RegistrationEventRow> list(String status, String stage, int limit) {
        StringBuilder sql=new StringBuilder("SELECT * FROM registration_events WHERE 1=1"); List<Object> args=new ArrayList<>();
        if(status!=null&&!status.isBlank()){sql.append(" AND status=?");args.add(status);}
        if(stage!=null&&!stage.isBlank()){sql.append(" AND stage=?");args.add(stage);}
        sql.append(" ORDER BY occurred_at DESC LIMIT ?");args.add(Math.max(1,Math.min(limit,500)));
        return jdbc.query(sql.toString(),(rs,n)->new RegistrationEventRow(rs.getLong("id"),rs.getString("request_id"),rs.getString("phone_masked"),rs.getString("stage"),rs.getString("status"),rs.getString("code"),rs.getString("details"),rs.getString("client"),rs.getObject("occurred_at",OffsetDateTime.class)),args.toArray());
    }

    @Scheduled(fixedDelay = 60_000L, initialDelay = 60_000L)
    public void monitor() {
        markMissingDeliveryReports();
        Integer errors=jdbc.queryForObject("SELECT COUNT(*) FROM registration_events WHERE occurred_at>=NOW()-INTERVAL '15 minutes' AND status IN ('ERROR','DELIVERY_FAILED')",Integer.class);
        if(errors!=null&&errors>=5) alertOnce("SMS_ERRORS","Неудачных SMS за 15 минут: "+errors);
        Integer offline=jdbc.queryForObject("SELECT COUNT(*) FROM registration_events WHERE occurred_at>=NOW()-INTERVAL '5 minutes' AND code='GATEWAY_OFFLINE'",Integer.class);
        if(offline!=null&&offline>0) alertOnce("GATEWAY_OFFLINE","SMS-шлюз недоступен");
    }

    private void markMissingDeliveryReports() {
        jdbc.update("INSERT INTO registration_events(request_id,phone_masked,stage,status,code,details,client) SELECT request_id,phone_masked,'DELIVERY','WARNING','DELIVERY_UNKNOWN','Отчёт о доставке не получен за 30 минут',client FROM registration_events e WHERE e.status='OPERATOR_ACCEPTED' AND e.occurred_at<NOW()-INTERVAL '30 minutes' AND NOT EXISTS (SELECT 1 FROM registration_events x WHERE x.request_id=e.request_id AND x.status IN ('DELIVERED','DELIVERY_FAILED','DELIVERY_UNKNOWN'))");
    }

    private void alertOnce(String type,String details) {
        Boolean recent=jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM admin_alert_log WHERE alert_type=? AND created_at>=NOW()-INTERVAL '30 minutes')",Boolean.class,type);
        if(Boolean.TRUE.equals(recent))return;
        jdbc.update("INSERT INTO admin_alert_log(alert_type,details) VALUES (?,?)",type,details);
        users.findAll().stream().filter(u->Boolean.TRUE.equals(u.getIsSuperAdmin())).forEach(u->notifications.send(u.getId(),NotificationType.ADMIN_MESSAGE,"Внимание: регистрация и SMS",details,null,Map.of("alertType",type)));
    }

    private static String maskPhone(String phone) {
        if(phone==null||phone.isBlank())return null; String digits=phone.replaceAll("\\D",""); if(digits.length()<4)return "•••";
        return "+"+digits.substring(0,Math.min(1,digits.length()))+" ••• •• "+digits.substring(digits.length()-2);
    }
    private static String safe(String value,int max){if(value==null)return null;String v=value.trim();return v.length()>max?v.substring(0,max):v;}
    private static String sanitize(String value){if(value==null)return null;return safe(value.replaceAll("\\b\\d{4,8}\\b","[скрыто]"),1000);}
}
