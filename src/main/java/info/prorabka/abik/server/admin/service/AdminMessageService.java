package info.prorabka.abik.server.admin.service;

import info.prorabka.abik.server.admin.dto.AdminDtos.MessageRow;
import info.prorabka.abik.server.admin.dto.AdminMessageRequest;
import info.prorabka.abik.server.common.exception.BadRequestException;
import info.prorabka.abik.server.notification.entity.NotificationType;
import info.prorabka.abik.server.notification.service.NotificationService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminMessageService {
    private final JdbcTemplate jdbc;
    private final NotificationService notifications;

    public AdminMessageService(JdbcTemplate jdbc, NotificationService notifications) {
        this.jdbc = jdbc; this.notifications = notifications;
    }

    @Transactional
    public MessageRow send(Long senderId, AdminMessageRequest request) {
        String link = trim(request.linkUrl());
        if (link != null) {
            try {
                URI uri = URI.create(link);
                if (!List.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null) throw new Exception();
            } catch (Exception e) { throw new BadRequestException("Ссылка должна начинаться с http:// или https://"); }
        }
        String position = trim(request.position());
        if (position != null && !List.of("GOALKEEPER","DEFENDER","FORWARD","TRAINER").contains(position)) {
            throw new BadRequestException("Неизвестное амплуа");
        }
        List<Long> recipients = recipients(request, position);
        if (recipients.isEmpty()) throw new BadRequestException("По выбранным фильтрам нет активных получателей");
        Long id = jdbc.queryForObject("INSERT INTO admin_messages(sender_user_id,title,body,link_url,region_id,city_id,team_id,position,recipient_count) VALUES (?,?,?,?,?,?,?,?,?) RETURNING id",Long.class,
                senderId,request.title().trim(),request.body().trim(),link,request.regionId(),request.cityId(),request.teamId(),position,recipients.size());
        if (id == null) throw new IllegalStateException("Не удалось создать рассылку");
        for (Long userId : recipients) {
            jdbc.update("INSERT INTO admin_message_recipients(message_id,user_id) VALUES (?,?)",id,userId);
            Map<String,Object> payload = new HashMap<>(); payload.put("adminMessageId",id);
            if (link != null) payload.put("linkUrl",link);
            notifications.send(userId,NotificationType.ADMIN_MESSAGE,request.title().trim(),request.body().trim(),request.teamId(),payload);
            jdbc.update("UPDATE admin_message_recipients SET notification_id=(SELECT n.id FROM notifications n WHERE n.user_id=? AND n.payload_json->>'adminMessageId'=? ORDER BY n.id DESC LIMIT 1) WHERE message_id=? AND user_id=?",userId,id.toString(),id,userId);
        }
        return history().stream().filter(row -> row.id().equals(id)).findFirst().orElseThrow();
    }

    public long countRecipients(AdminMessageRequest request) { return recipients(request,trim(request.position())).size(); }

    private List<Long> recipients(AdminMessageRequest request, String position) {
        StringBuilder sql = new StringBuilder("SELECT DISTINCT u.id FROM users u JOIN user_profiles p ON p.user_id=u.id JOIN cities c ON c.id=p.city_id JOIN team_members tm ON tm.user_id=u.id AND tm.status='ACTIVE' JOIN teams t ON t.id=tm.team_id AND t.status<>'ARCHIVED' WHERE u.status='ACTIVE'");
        List<Object> args = new ArrayList<>();
        if (request.regionId()!=null) { sql.append(" AND c.region_id=?"); args.add(request.regionId()); }
        if (request.cityId()!=null) { sql.append(" AND c.id=?"); args.add(request.cityId()); }
        if (request.teamId()!=null) { sql.append(" AND t.id=?"); args.add(request.teamId()); }
        if ("TRAINER".equals(position)) sql.append(" AND tm.is_trainer=TRUE");
        else if (position!=null) { sql.append(" AND EXISTS (SELECT 1 FROM user_positions up WHERE up.user_id=u.id AND up.position=?)"); args.add(position); }
        sql.append(" ORDER BY u.id");
        return jdbc.queryForList(sql.toString(),Long.class,args.toArray());
    }

    public List<MessageRow> history() {
        String sql = "SELECT m.*,u.phone author,CONCAT_WS(' • ',COALESCE(r.name,'Все регионы'),COALESCE(ci.name,'Все города'),COALESCE(t.name,'Все команды'),COALESCE(m.position,'Все амплуа')) audience," +
                " (SELECT COUNT(*) FROM admin_message_recipients mr JOIN notifications n ON n.id=mr.notification_id WHERE mr.message_id=m.id AND n.is_read=TRUE) read_count" +
                " FROM admin_messages m JOIN users u ON u.id=m.sender_user_id LEFT JOIN regions r ON r.id=m.region_id LEFT JOIN cities ci ON ci.id=m.city_id LEFT JOIN teams t ON t.id=m.team_id ORDER BY m.created_at DESC LIMIT 200";
        return jdbc.query(sql,(rs,n)->new MessageRow(rs.getLong("id"),rs.getString("title"),rs.getString("body"),rs.getString("link_url"),rs.getString("audience"),rs.getLong("recipient_count"),rs.getLong("read_count"),rs.getObject("created_at",OffsetDateTime.class),rs.getString("author")));
    }

    private static String trim(String value) { if(value==null)return null; String trimmed=value.trim(); return trimmed.isEmpty()?null:trimmed; }
}
