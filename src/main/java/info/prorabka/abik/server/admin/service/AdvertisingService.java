package info.prorabka.abik.server.admin.service;

import info.prorabka.abik.server.admin.dto.AdminDtos.*;
import info.prorabka.abik.server.admin.dto.AdvertisementEventRequest;
import info.prorabka.abik.server.admin.dto.AdvertisementRequest;
import info.prorabka.abik.server.common.exception.BadRequestException;
import info.prorabka.abik.server.common.exception.ForbiddenException;
import info.prorabka.abik.server.common.exception.NotFoundException;
import info.prorabka.abik.server.common.file.FileStorageService;
import info.prorabka.abik.server.user.entity.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdvertisingService {
    private final JdbcTemplate jdbc;
    private final FileStorageService files;

    public AdvertisingService(JdbcTemplate jdbc, FileStorageService files) { this.jdbc=jdbc; this.files=files; }

    public List<AdvertisementRow> list(User actor) {
        boolean all=Boolean.TRUE.equals(actor.getIsSuperAdmin());
        String sql=select()+" WHERE a.deleted_at IS NULL"+(all?"":" AND a.creator_user_id=?")+" ORDER BY a.created_at DESC";
        return all?jdbc.query(sql,(rs,n)->mapAd(rs)):jdbc.query(sql,(rs,n)->mapAd(rs),actor.getId());
    }

    public AdvertisementRow get(User actor,Long id) {
        AdvertisementRow ad=find(id);
        if(!Boolean.TRUE.equals(actor.getIsSuperAdmin())&&!ad.creatorUserId().equals(actor.getId()))throw new ForbiddenException("Можно управлять только своими публикациями");
        return ad;
    }

    @Transactional
    public AdvertisementRow create(User actor,AdvertisementRequest request) {
        ValidatedAd v=validate(request);
        Long id=jdbc.queryForObject("INSERT INTO advertisements(creator_user_id,title,body,image_url,link_url,button_text,target_type,target_id,starts_at,ends_at,active) VALUES (?,?,?,?,?,?,?,?,?,?,?) RETURNING id",Long.class,
                actor.getId(),v.title(),v.body(),v.image(),v.link(),v.button(),v.targetType(),v.targetId(),request.startsAt(),request.endsAt(),request.active()==null||request.active());
        return find(id);
    }

    @Transactional
    public AdvertisementRow update(User actor,Long id,AdvertisementRequest request) {
        AdvertisementRow old=get(actor,id); ValidatedAd v=validate(request);
        jdbc.update("UPDATE advertisements SET title=?,body=?,image_url=?,link_url=?,button_text=?,target_type=?,target_id=?,starts_at=?,ends_at=?,active=?,updated_at=NOW() WHERE id=?",
                v.title(),v.body(),v.image(),v.link(),v.button(),v.targetType(),v.targetId(),request.startsAt(),request.endsAt(),request.active()==null||request.active(),id);
        if(old.imageUrl()!=null&&!old.imageUrl().equals(v.image()))files.delete(old.imageUrl());
        return find(id);
    }

    @Transactional
    public AdvertisementRow setActive(User actor,Long id,boolean active) {
        get(actor,id); jdbc.update("UPDATE advertisements SET active=?,updated_at=NOW() WHERE id=?",active,id); return find(id);
    }

    @Transactional
    public void delete(User actor,Long id) {
        AdvertisementRow old=get(actor,id);
        jdbc.update("UPDATE advertisements SET deleted_at=NOW(),active=FALSE,updated_at=NOW() WHERE id=?",id);
        files.delete(old.imageUrl());
    }

    /** Одна публикация: команда → город → регион → все, затем самая новая. */
    public AdvertisementRow activeForUser(Long userId,Long selectedTeamId) {
        Viewer viewer=viewer(userId,selectedTeamId);
        StringBuilder sql=new StringBuilder(select()+" WHERE a.deleted_at IS NULL AND a.active=TRUE AND a.starts_at<=NOW() AND a.ends_at>=NOW() AND (a.target_type='GLOBAL'");
        List<Object> args=new ArrayList<>();
        if(viewer.regionId()!=null){sql.append(" OR (a.target_type='REGION' AND a.target_id=?)");args.add(viewer.regionId());}
        if(viewer.cityId()!=null){sql.append(" OR (a.target_type='CITY' AND a.target_id=?)");args.add(viewer.cityId());}
        if(viewer.teamId()!=null){sql.append(" OR (a.target_type='TEAM' AND a.target_id=?)");args.add(viewer.teamId());}
        sql.append(") ORDER BY CASE a.target_type WHEN 'TEAM' THEN 4 WHEN 'CITY' THEN 3 WHEN 'REGION' THEN 2 ELSE 1 END DESC,a.created_at DESC,a.id DESC LIMIT 1");
        List<AdvertisementRow> rows=jdbc.query(sql.toString(),(rs,n)->mapAd(rs),args.toArray());
        return rows.stream().findFirst().orElse(null);
    }

    @Transactional
    public void recordEvent(Long userId,Long adId,AdvertisementEventRequest request) {
        String type=request.type().toUpperCase(); if(!List.of("VIEW","CLICK").contains(type))throw new BadRequestException("Тип события: VIEW или CLICK");
        AdvertisementRow visible=activeForUser(userId,request.teamId());
        if(visible==null||!visible.id().equals(adId))throw new ForbiddenException("Публикация недоступна этому пользователю");
        Viewer viewer=viewer(userId,request.teamId());
        String sql="INSERT INTO advertisement_events(ad_id,user_id,event_type,session_id,region_id,city_id,team_id) VALUES (?,?,?,?,?,?,?)";
        if("VIEW".equals(type))sql+=" ON CONFLICT (ad_id,user_id,session_id) WHERE event_type='VIEW' DO NOTHING";
        jdbc.update(sql,adId,userId,type,request.sessionId().trim(),viewer.regionId(),viewer.cityId(),viewer.teamId());
    }

    public AdStats stats(User actor,Long adId,LocalDate from,LocalDate to,Long regionId,Long cityId,Long teamId) {
        get(actor,adId);
        StringBuilder condition=new StringBuilder(" WHERE e.ad_id=?"); List<Object> args=new ArrayList<>();args.add(adId);
        if(from!=null){condition.append(" AND (e.occurred_at AT TIME ZONE 'UTC')::date>=?");args.add(from);}
        if(to!=null){condition.append(" AND (e.occurred_at AT TIME ZONE 'UTC')::date<=?");args.add(to);}
        if(regionId!=null){condition.append(" AND e.region_id=?");args.add(regionId);}
        if(cityId!=null){condition.append(" AND e.city_id=?");args.add(cityId);}
        if(teamId!=null){condition.append(" AND e.team_id=?");args.add(teamId);}
        String aggregate=" COUNT(*) FILTER(WHERE e.event_type='VIEW') views, COUNT(DISTINCT e.user_id) FILTER(WHERE e.event_type='VIEW') viewers, COUNT(*) FILTER(WHERE e.event_type='CLICK') clicks, COUNT(DISTINCT e.user_id) FILTER(WHERE e.event_type='CLICK') unique_clicks";
        var totals=jdbc.queryForMap("SELECT"+aggregate+" FROM advertisement_events e"+condition,args.toArray());
        String geoSql="SELECT e.region_id,r.name region_name,e.city_id,c.name city_name,e.team_id,t.name team_name,"+aggregate+" FROM advertisement_events e LEFT JOIN regions r ON r.id=e.region_id LEFT JOIN cities c ON c.id=e.city_id LEFT JOIN teams t ON t.id=e.team_id"+condition+" GROUP BY e.region_id,r.name,e.city_id,c.name,e.team_id,t.name ORDER BY views DESC";
        List<AdGeoRow> geography=jdbc.query(geoSql,(rs,n)->new AdGeoRow(nullable(rs,"region_id"),rs.getString("region_name"),nullable(rs,"city_id"),rs.getString("city_name"),nullable(rs,"team_id"),rs.getString("team_name"),rs.getLong("views"),rs.getLong("viewers"),rs.getLong("clicks"),rs.getLong("unique_clicks")),args.toArray());
        return new AdStats(adId,new AdTotals(number(totals.get("views")),number(totals.get("viewers")),number(totals.get("clicks")),number(totals.get("unique_clicks"))),geography);
    }

    private ValidatedAd validate(AdvertisementRequest r) {
        String title=trim(r.title()),body=trim(r.body()),image=trim(r.imageUrl()),link=trim(r.linkUrl()),button=trim(r.buttonText()),type=trim(r.targetType());
        if(title==null&&body==null&&image==null)throw new BadRequestException("Заполните заголовок, текст или добавьте изображение");
        if(r.startsAt()==null||r.endsAt()==null)throw new BadRequestException("Укажите даты начала и окончания");
        if(!r.endsAt().isAfter(r.startsAt()))throw new BadRequestException("Окончание должно быть позже начала");
        if(link!=null){
            if(button==null)throw new BadRequestException("Для ссылки укажите текст кнопки перехода");
            try{URI uri=URI.create(link);if(!List.of("http","https").contains(uri.getScheme())||uri.getHost()==null)throw new Exception();}catch(Exception e){throw new BadRequestException("Некорректная ссылка");}
        }
        if(type==null||!List.of("GLOBAL","REGION","CITY","TEAM").contains(type))throw new BadRequestException("Выберите аудиторию показа");
        Long targetId=r.targetId();
        if("GLOBAL".equals(type))targetId=null;
        else{
            if(targetId==null)throw new BadRequestException("Выберите значение таргетинга");
            String table=switch(type){case "REGION"->"regions";case "CITY"->"cities";case "TEAM"->"teams";default->throw new BadRequestException("Неизвестный таргетинг");};
            Boolean exists=jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM "+table+" WHERE id=?)",Boolean.class,targetId);
            if(!Boolean.TRUE.equals(exists))throw new BadRequestException("Выбранная аудитория не найдена");
        }
        return new ValidatedAd(title,body,image,link,button,type,targetId);
    }

    private Viewer viewer(Long userId,Long selectedTeamId) {
        String sql="SELECT p.city_id,c.region_id,"+
                " (SELECT tm.team_id FROM team_members tm JOIN teams t ON t.id=tm.team_id WHERE tm.user_id=? AND tm.status='ACTIVE' AND t.status='ACTIVE' AND (?::bigint IS NULL OR tm.team_id=?::bigint) ORDER BY (tm.team_id=?::bigint) DESC,tm.joined_at DESC LIMIT 1) team_id"+
                " FROM users u LEFT JOIN user_profiles p ON p.user_id=u.id LEFT JOIN cities c ON c.id=p.city_id WHERE u.id=? AND u.status='ACTIVE'";
        return jdbc.query(sql,(rs,n)->new Viewer(nullable(rs,"region_id"),nullable(rs,"city_id"),nullable(rs,"team_id")),userId,selectedTeamId,selectedTeamId,selectedTeamId,userId).stream().findFirst().orElseThrow(()->new NotFoundException("Пользователь не найден"));
    }

    private AdvertisementRow find(Long id) {
        return jdbc.query(select()+" WHERE a.id=? AND a.deleted_at IS NULL",(rs,n)->mapAd(rs),id).stream().findFirst().orElseThrow(()->new NotFoundException("Публикация не найдена"));
    }

    private String select() {
        return "SELECT a.*,COALESCE(NULLIF(BTRIM(CONCAT_WS(' ',p.last_name,p.first_name)),''),u.phone) creator_name,"+
                " CASE a.target_type WHEN 'GLOBAL' THEN 'Все пользователи' WHEN 'REGION' THEN (SELECT name FROM regions WHERE id=a.target_id) WHEN 'CITY' THEN (SELECT name FROM cities WHERE id=a.target_id) WHEN 'TEAM' THEN (SELECT name FROM teams WHERE id=a.target_id) END target_name"+
                " FROM advertisements a JOIN users u ON u.id=a.creator_user_id LEFT JOIN user_profiles p ON p.user_id=u.id";
    }

    private AdvertisementRow mapAd(ResultSet rs)throws SQLException{
        return new AdvertisementRow(rs.getLong("id"),rs.getString("title"),rs.getString("body"),rs.getString("image_url"),rs.getString("link_url"),rs.getString("button_text"),rs.getString("target_type"),nullable(rs,"target_id"),rs.getString("target_name"),rs.getObject("starts_at",OffsetDateTime.class),rs.getObject("ends_at",OffsetDateTime.class),rs.getBoolean("active"),rs.getLong("creator_user_id"),rs.getString("creator_name"),rs.getObject("created_at",OffsetDateTime.class));
    }
    private record Viewer(Long regionId,Long cityId,Long teamId){}
    private record ValidatedAd(String title,String body,String image,String link,String button,String targetType,Long targetId){}
    private static String trim(String value){if(value==null)return null;String v=value.trim();return v.isEmpty()?null:v;}
    private static Long nullable(ResultSet rs,String column)throws SQLException{long value=rs.getLong(column);return rs.wasNull()?null:value;}
    private static long number(Object value){return value instanceof Number n?n.longValue():0;}
}
