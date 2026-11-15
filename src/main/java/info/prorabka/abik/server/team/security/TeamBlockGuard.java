package info.prorabka.abik.server.team.security;

import info.prorabka.abik.server.common.exception.ForbiddenException;
import info.prorabka.abik.server.team.entity.Team;
import info.prorabka.abik.server.team.entity.TeamStatus;
import info.prorabka.abik.server.team.repository.TeamRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

/** Заблокированная команда доступна участникам только для чтения истории. */
@Component
public class TeamBlockGuard implements HandlerInterceptor {
    private final TeamRepository teams;

    public TeamBlockGuard(TeamRepository teams) { this.teams = teams; }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        if ("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod())) return true;
        Object attribute = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(attribute instanceof Map<?, ?> variables)) return true;
        Object rawTeamId = variables.get("teamId");
        if (rawTeamId == null) return true;
        Long teamId = Long.valueOf(String.valueOf(rawTeamId));
        Team team = teams.findById(teamId).orElse(null);
        if (team != null && team.getStatus() == TeamStatus.BLOCKED) {
            String reason = team.getBlockReason();
            throw new ForbiddenException("Команда заблокирована. Доступен только просмотр истории."
                    + (reason == null || reason.isBlank() ? "" : " Причина: " + reason));
        }
        return true;
    }
}
