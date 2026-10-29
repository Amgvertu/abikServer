package info.prorabka.abik.server.team.service;

import info.prorabka.abik.server.common.exception.ForbiddenException;
import info.prorabka.abik.server.common.exception.NotFoundException;
import info.prorabka.abik.server.notification.service.NotificationService;
import info.prorabka.abik.server.team.dto.MemberDetailsResponse;
import info.prorabka.abik.server.team.dto.SetJerseyNumberRequest;
import info.prorabka.abik.server.team.dto.TeamMemberResponse;
import info.prorabka.abik.server.team.dto.UpdateMemberRoleRequest;
import info.prorabka.abik.server.user.dto.UserProfileRequest;
import info.prorabka.abik.server.user.dto.UserProfileResponse;
import info.prorabka.abik.server.user.entity.UserPosition;
import info.prorabka.abik.server.user.service.UserProfileService;
import info.prorabka.abik.server.team.entity.*;
import info.prorabka.abik.server.team.repository.TeamMemberRepository;
import info.prorabka.abik.server.user.entity.User;
import info.prorabka.abik.server.user.repository.UserProfileRepository;
import info.prorabka.abik.server.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Сервис участников команд: роли, статусы, игровые номера, проверки прав.
 */
@Service
public class TeamMemberService {

    private static final Logger log = LoggerFactory.getLogger(TeamMemberService.class);

    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final NotificationService notificationService;
    private final UserProfileService userProfileService;

    public TeamMemberService(TeamMemberRepository teamMemberRepository,
                             UserRepository userRepository,
                             UserProfileRepository userProfileRepository,
                             NotificationService notificationService,
                             UserProfileService userProfileService) {
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.notificationService = notificationService;
        this.userProfileService = userProfileService;
    }

    // ============================================================
    // Проверки прав
    // ============================================================

    /** Активный участник команды. Иначе — 403. */
    public TeamMember requireMember(Long teamId, Long userId) {
        return teamMemberRepository
                .findByTeamIdAndUserIdAndStatus(teamId, userId, MemberStatus.ACTIVE)
                .orElseThrow(() -> new ForbiddenException("Вы не являетесь участником команды"));
    }

    /** Владелец или модератор. Иначе — 403. */
    public TeamMember requireManager(Long teamId, Long userId) {
        TeamMember member = requireMember(teamId, userId);
        if (member.getRole() != MemberRole.OWNER
                && member.getRole() != MemberRole.ADMIN           // ← добавить
                && member.getRole() != MemberRole.MODERATOR) {
            throw new ForbiddenException("Недостаточно прав: нужна роль владельца, админа или модератора");
        }
        return member;
    }

    /** Только владелец. Иначе — 403. */
    public TeamMember requireOwner(Long teamId, Long userId) {
        TeamMember member = requireMember(teamId, userId);
        if (member.getRole() != MemberRole.OWNER) {
            throw new ForbiddenException("Только владелец команды может выполнить это действие");
        }
        return member;
    }

    /** Вернуть роль пользователя в команде или null, если не участник. */
    public MemberRole getRoleOrNull(Long teamId, Long userId) {
        return teamMemberRepository.findByTeamIdAndUserIdAndStatus(teamId, userId, MemberStatus.ACTIVE)
                .map(TeamMember::getRole)
                .orElse(null);
    }

    // ============================================================
    // Добавление участника
    // ============================================================

    /**
     * Добавить нового активного участника. Используется при создании команды и при принятии приглашения.
     * Если запись уже существует (была исключена/вышел) — реактивируем её.
     */
    @Transactional
    public TeamMember addMember(Long teamId, Long userId, MemberRole role) {
        TeamMember existing = teamMemberRepository.findByTeamIdAndUserId(teamId, userId).orElse(null);

        if (existing != null) {
            // Реактивация
            existing.setRole(role);
            existing.setStatus(MemberStatus.ACTIVE);
            existing.setJoinedAt(OffsetDateTime.now());
            existing.setLeftAt(null);
            existing.setExcludedAt(null);
            return teamMemberRepository.save(existing);
        }

        TeamMember member = TeamMember.builder()
                .teamId(teamId)
                .userId(userId)
                .role(role)
                .status(MemberStatus.ACTIVE)
                .joinedAt(OffsetDateTime.now())
                .build();
        return teamMemberRepository.save(member);
    }

    // ============================================================
    // Управление участниками
    // ============================================================

    /** Назначить/снять роль MODERATOR. Владельцем через этот метод нельзя. */
    @Transactional
    public TeamMemberResponse changeRole(Long teamId, Long targetUserId, Long actorUserId,
                                         UpdateMemberRoleRequest req) {
        requireOwner(teamId, actorUserId);

        if (req.role() == MemberRole.OWNER) {
            throw new ForbiddenException("Для передачи владения используйте отдельный endpoint");
        }
// ADMIN может назначать только владелец
        if (req.role() == MemberRole.ADMIN && requireOwner(teamId, actorUserId).getRole() != MemberRole.OWNER) {
            throw new ForbiddenException("Назначать админов может только владелец");
        }

        TeamMember target = requireMember(teamId, targetUserId);
        target.setRole(req.role());
        TeamMember saved = teamMemberRepository.save(target);
        log.info("Роль изменена: teamId={}, userId={}, newRole={}", teamId, targetUserId, req.role());
        notificationService.notifyRoleChanged(targetUserId, teamId, "команда #" + teamId, req.role().name());
        return toResponse(saved);
    }

    /** Установить или снять игровой номер. */
    @Transactional
    public TeamMemberResponse setJerseyNumber(Long teamId, Long targetUserId, Long actorUserId,
                                              SetJerseyNumberRequest req) {
        requireManager(teamId, actorUserId);

        TeamMember target = requireMember(teamId, targetUserId);

        if (req.jerseyNumber() != null) {
            // Проверяем занятость номера среди активных
            teamMemberRepository
                    .findByTeamIdAndJerseyNumberAndStatus(teamId, req.jerseyNumber(), MemberStatus.ACTIVE)
                    .ifPresent(other -> {
                        if (!other.getId().equals(target.getId())) {
                            throw new ForbiddenException(
                                    "Номер " + req.jerseyNumber() + " уже занят в команде");
                        }
                    });
        }

        target.setJerseyNumber(req.jerseyNumber());
        TeamMember saved = teamMemberRepository.save(target);
        return toResponse(saved);
    }

    /** Исключить участника. Долги сохраняются, статус EXCLUDED. */
    @Transactional
    public void excludeMember(Long teamId, Long targetUserId, Long actorUserId) {
        TeamMember actor = requireManager(teamId, actorUserId);
        TeamMember target = requireMember(teamId, targetUserId);

        // Модератор не может исключить владельца или другого модератора
        if (actor.getRole() == MemberRole.MODERATOR &&
                (target.getRole() == MemberRole.OWNER || target.getRole() == MemberRole.MODERATOR)) {
            throw new ForbiddenException("Модератор не может исключить владельца или модератора");
        }

        if (target.getRole() == MemberRole.OWNER) {
            throw new ForbiddenException("Владелец не может быть исключён. Сначала передайте владение.");
        }

        target.setStatus(MemberStatus.EXCLUDED);
        target.setExcludedAt(OffsetDateTime.now());
        target.setJerseyNumber(null);
        teamMemberRepository.save(target);
        notificationService.notifyExcluded(targetUserId, teamId, "команда #" + teamId);
        log.info("Участник исключён: teamId={}, userId={}", teamId, targetUserId);
    }

    /** Выход из команды (сам). Владелец не может выйти без передачи прав или архивации. */
    @Transactional
    public void leaveTeam(Long teamId, Long userId) {
        TeamMember member = requireMember(teamId, userId);

        if (member.getRole() == MemberRole.OWNER) {
            throw new ForbiddenException(
                    "Владелец не может покинуть команду. Передайте владение или архивируйте команду.");
        }

        member.setStatus(MemberStatus.LEFT);
        member.setLeftAt(OffsetDateTime.now());
        member.setJerseyNumber(null);
        teamMemberRepository.save(member);
        log.info("Пользователь вышел из команды: teamId={}, userId={}", teamId, userId);
    }

    /** Вернуть исключённого участника (или вышедшего) — ACTIVE. */
    @Transactional
    public TeamMemberResponse restoreMember(Long teamId, Long targetUserId, Long actorUserId) {
        requireManager(teamId, actorUserId);

        TeamMember target = teamMemberRepository.findByTeamIdAndUserId(teamId, targetUserId)
                .orElseThrow(() -> new NotFoundException("Участник не найден в команде"));

        if (target.getStatus() == MemberStatus.ACTIVE) {
            throw new ForbiddenException("Участник уже активен");
        }

        target.setStatus(MemberStatus.ACTIVE);
        target.setJoinedAt(OffsetDateTime.now());
        target.setLeftAt(null);
        target.setExcludedAt(null);
        TeamMember saved = teamMemberRepository.save(target);
        return toResponse(saved);
    }

    // ============================================================
    // Чтение
    // ============================================================

    /** Все активные участники команды. */
    public List<TeamMemberResponse> listActiveMembers(Long teamId) {
        return teamMemberRepository
                .findByTeamIdAndStatusOrderByJoinedAtAsc(teamId, MemberStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Число активных участников. */
    public long countActive(Long teamId) {
        return teamMemberRepository
                .findByTeamIdAndStatusOrderByJoinedAtAsc(teamId, MemberStatus.ACTIVE).size();
    }

    // ============================================================
    // Helpers
    // ============================================================

    private TeamMemberResponse toResponse(TeamMember m) {
        User user = userRepository.findById(m.getUserId()).orElse(null);
        String phone = user != null ? user.getPhone() : null;

        String lastName = null;
        String firstName = null;
        Set<UserPosition> positions = Set.of();
        String avatarUrl = null;
        var profileOpt = userProfileRepository.findById(m.getUserId());
        if (profileOpt.isPresent()) {
            var p = profileOpt.get();
            lastName = p.getLastName();
            firstName = p.getFirstName();
            positions = new HashSet<>(p.getPositions());
            avatarUrl = p.getAvatarUrl();
        }

        return TeamMemberResponse.from(m, phone, lastName, firstName, positions, avatarUrl);
    }

    /** Проверить, является ли пользователь суперадмином. */
    public boolean isSuperAdmin(Long userId) {
        return userRepository.findById(userId)
                .map(User::getIsSuperAdmin)
                .orElse(false);
    }

    /** Проверка прав: владелец, модератор ИЛИ суперадмин. */
    public void requireManagerOrSuperAdmin(Long teamId, Long userId) {
        if (isSuperAdmin(userId)) {
            return; // Суперадмин может всё
        }
        requireManager(teamId, userId);
    }

    @Transactional
    public TeamMemberResponse setTrainer(Long teamId, Long targetUserId, Long actorUserId,
                                         boolean isTrainer) {
        TeamMember actor = requireManager(teamId, actorUserId);
        // Тренера назначает только OWNER или ADMIN
        if (actor.getRole() != MemberRole.OWNER && actor.getRole() != MemberRole.ADMIN) {
            throw new ForbiddenException("Назначать тренера может только владелец или админ");
        }
        TeamMember target = requireMember(teamId, targetUserId);
        target.setIsTrainer(isTrainer);
        TeamMember saved = teamMemberRepository.save(target);
        log.info("Тренер обновлён: teamId={}, userId={}, isTrainer={}",
                teamId, targetUserId, isTrainer);
        notificationService.notifyRoleChanged(targetUserId, teamId,
                "команда #" + teamId, isTrainer ? "TRAINER" : "не тренер");
        return toResponse(saved);
    }

    // ============================================================
    // Защищённая карточка участника (ТЗ 9.2, BE-07)
    // ============================================================

    /**
     * Карточка участника в контексте команды.
     * Доступ: любой активный участник той же команды (посторонний — 403).
     * Цель — участник команды в любом статусе: карточка исключённого нужна
     * управляющим, т.к. его долги и платежи сохраняются.
     * Профиль может отсутствовать — тогда profile = null.
     */
    public MemberDetailsResponse getMemberDetails(Long teamId, Long targetUserId, Long actorUserId) {
        requireMember(teamId, actorUserId);

        TeamMember target = teamMemberRepository
                .findByTeamIdAndUserId(teamId, targetUserId)
                .orElseThrow(() -> new NotFoundException("Участник не найден в команде"));

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        UserProfileResponse profile = userProfileService.exists(targetUserId)
                ? userProfileService.getByUserId(targetUserId)
                : null;

        return new MemberDetailsResponse(
                target.getUserId(),
                user.getPhone(),
                target.getRole(),
                target.getStatus(),
                target.getJerseyNumber(),
                target.getIsTrainer(),
                target.getJoinedAt(),
                profile
        );
    }

    /**
     * Редактирование профиля участника управляющим команды (BE-15b).
     * Доступ: владелец/модератор команды или суперадмин.
     * Цель — участник команды (в любом статусе членства).
     * Тело запроса и валидация — те же, что у PUT /v1/users/me/profile.
     */
    @Transactional
    public UserProfileResponse updateMemberProfile(Long teamId, Long targetUserId, Long actorUserId,
                                                   UserProfileRequest req) {
        requireManagerOrSuperAdmin(teamId, actorUserId);
        requireMember(teamId, targetUserId);
        UserProfileResponse resp = userProfileService.createOrUpdate(targetUserId, req);
        log.info("Профиль участника обновлён управляющим: teamId={}, targetUserId={}, actor={}",
                teamId, targetUserId, actorUserId);
        return resp;
    }
}