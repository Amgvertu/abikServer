package info.prorabka.abik.server.team.security;

import info.prorabka.abik.server.common.exception.ForbiddenException;
import info.prorabka.abik.server.team.entity.Team;
import info.prorabka.abik.server.team.entity.TeamStatus;
import info.prorabka.abik.server.team.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamBlockGuardTest {
    @Mock TeamRepository teams;
    @InjectMocks TeamBlockGuard guard;

    @Test void blockedTeamIsReadOnly() {
        MockHttpServletRequest read = request("GET");
        assertTrue(guard.preHandle(read, new MockHttpServletResponse(), new Object()));
        MockHttpServletRequest write = request("POST");
        when(teams.findById(5L)).thenReturn(Optional.of(Team.builder().status(TeamStatus.BLOCKED).blockReason("Проверка").build()));
        assertThrows(ForbiddenException.class, () -> guard.preHandle(write, new MockHttpServletResponse(), new Object()));
    }

    @Test void activeTeamAllowsWrites() {
        MockHttpServletRequest request = request("POST");
        when(teams.findById(5L)).thenReturn(Optional.of(Team.builder().status(TeamStatus.ACTIVE).build()));
        assertTrue(guard.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    private MockHttpServletRequest request(String method) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/v1/teams/5/payments");
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("teamId", "5"));
        return request;
    }
}
