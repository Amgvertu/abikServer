package info.prorabka.abik.server.admin.service;

import info.prorabka.abik.server.admin.dto.AdminDtos.AdvertisementRow;
import info.prorabka.abik.server.admin.dto.AdvertisementEventRequest;
import info.prorabka.abik.server.common.file.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdvertisingServiceTest {
    @Mock JdbcTemplate jdbc;
    @Mock FileStorageService files;
    @InjectMocks AdvertisingService service;

    @Test void activeAdQueryUsesAudiencePriority() throws Exception {
        stubQueries(null);
        assertNull(service.activeForUser(1L, 30L));
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc, atLeastOnce()).query(sql.capture(), any(RowMapper.class), any(Object[].class));
        String activeQuery = sql.getAllValues().stream().filter(value -> value.contains("ORDER BY CASE")).findFirst().orElseThrow();
        assertTrue(activeQuery.contains("WHEN 'TEAM' THEN 4"));
        assertTrue(activeQuery.contains("WHEN 'CITY' THEN 3"));
        assertTrue(activeQuery.contains("WHEN 'REGION' THEN 2"));
        assertTrue(activeQuery.contains("a.starts_at<=NOW() AND a.ends_at>=NOW()"));
    }

    @Test void repeatedViewInSameSessionIsIdempotent() throws Exception {
        stubQueries(ad());
        service.recordEvent(1L, 7L, new AdvertisementEventRequest("session-1", "VIEW", 30L));
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(sql.capture(), any(Object[].class));
        assertTrue(sql.getValue().contains("ON CONFLICT (ad_id,user_id,session_id) WHERE event_type='VIEW' DO NOTHING"));
    }

    private void stubQueries(AdvertisementRow ad) throws Exception {
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("FROM users u LEFT JOIN user_profiles")) {
                ResultSet rs = mock(ResultSet.class);
                when(rs.getLong("region_id")).thenReturn(10L);
                when(rs.getLong("city_id")).thenReturn(20L);
                when(rs.getLong("team_id")).thenReturn(30L);
                when(rs.wasNull()).thenReturn(false);
                RowMapper<Object> mapper = invocation.getArgument(1);
                return List.of(mapper.mapRow(rs, 0));
            }
            return ad == null ? List.of() : List.of(ad);
        });
    }

    private AdvertisementRow ad() {
        OffsetDateTime now = OffsetDateTime.now();
        return new AdvertisementRow(7L, "Заголовок", null, null, null, null, "TEAM", 30L, "Команда",
                now.minusHours(1), now.plusHours(1), true, 2L, "Автор", now);
    }
}
