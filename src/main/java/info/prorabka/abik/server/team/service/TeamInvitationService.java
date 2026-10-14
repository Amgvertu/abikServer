package info.prorabka.abik.server.team.service;

import info.prorabka.abik.server.common.exception.BadRequestException;
import info.prorabka.abik.server.common.exception.ConflictException;
import info.prorabka.abik.server.common.exception.ForbiddenException;
import info.prorabka.abik.server.common.exception.NotFoundException;
import info.prorabka.abik.server.common.util.PhoneNormalizer;
import info.prorabka.abik.server.team.dto.InvitationResponse;
import info.prorabka.abik.server.team.entity.*;
import info.prorabka.abik.server.team.repository.TeamInvitationRepository;
import info.prorabka.abik.server.team.repository.TeamMemberRepository;
import info.prorabka.abik.server.team.repository.TeamRepository;
import info.prorabka.abik.server.user.entity.User;
import info.prorabka.abik.server.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Сервис приглашений в команду.
 */
@Service
public class TeamInvitationService {

    private static final Logger log = LoggerFactory.getLogger(TeamInvitationService.class);

    /** Срок жизни приглашения — 7 дней (по ТЗ). */
    private static final int INVITATION_TTL_DAYS = 7;

    private final TeamInvitationRepository invitationRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final TeamMemberService teamMemberService;

    public TeamInvitationService(TeamInvitationRepository invitationRepository,
                                 TeamMemberRepository teamMemberRepository,
                                 TeamRepository teamRepository,
                                 UserRepository userRepository,
                                 TeamMemberService teamMemberService) {
        this.invitationRepository = invitationRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.teamMemberService = teamMemberService;
    }

    // ============================================================
    // Создание приглашения
    // ============================================================

    /**
     * Владелец/модератор приглашает пользователя по номеру телефона.
     */
    @Transactional
    public InvitationResponse invite(Long teamId, Long actorUserId, String phoneRaw) {
        // Проверяем, что актор — владелец/модератор
        teamMemberService.requireManager(teamId, actorUserId);

        // Проверяем, что команда активна
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Команда не найдена"));
        if (team.getStatus() != TeamStatus.ACTIVE) {
            throw new ForbiddenException("Нельзя приглашать в архивную команду");
        }

        // Нормализуем телефон и ищем пользователя
        String phone = PhoneNormalizer.normalize(phoneRaw);
        User invited = userRepository.findByPhone(phone)
                .orElseThrow(() -> new NotFoundException(
                        "Пользователь с таким номером не зарегистрирован в системе"));

        // Проверяем, что пользователь не в команде
        if (teamMemberRepository.existsByTeamIdAndUserIdAndStatus(
                teamId, invited.getId(), MemberStatus.ACTIVE)) {
            throw new ConflictException("Пользователь уже является участником команды");
        }

        // Проверяем, что нет уже активного приглашения
        invitationRepository.findByTeamIdAndInvitedUserIdAndStatus(
                        teamId, invited.getId(), InvitationStatus.PENDING)
                .ifPresent(i -> { throw new ConflictException("Приглашение уже отправлено"); });

        // Создаём
        TeamInvitation invitation = TeamInvitation.builder()
                .teamId(teamId)
                .inviterUserId(actorUserId)
                .invitedUserId(invited.getId())
                .invitedPhone(phone)
                .status(InvitationStatus.PENDING)
                .expiresAt(OffsetDateTime.now().plusDays(INVITATION_TTL_DAYS))
                .build();

        TeamInvitation saved = invitationRepository.save(invitation);
        log.info("Приглашение создано: teamId={}, invitedUserId={}, by={}",
                teamId, invited.getId(), actorUserId);

        return InvitationResponse.from(saved, team.getName());
    }

    // ============================================================
    // Просмотр
    // ============================================================

    /** Все PENDING-приглашения пользователю (входящие). */
    @Transactional
    public List<InvitationResponse> listMyIncoming(Long userId) {
        expireOverdue();  // обновляем просроченные
        return invitationRepository
                .findByInvitedUserIdAndStatusOrderByCreatedAtDesc(userId, InvitationStatus.PENDING)
                .stream()
                .map(this::toResponseWithTeamName)
                .toList();
    }

    /** Все PENDING-приглашения, отправленные из команды (исходящие). */
    @Transactional
    public List<InvitationResponse> listTeamOutgoing(Long teamId, Long actorUserId) {
        teamMemberService.requireManager(teamId, actorUserId);
        expireOverdue();
        return invitationRepository
                .findByTeamIdAndStatusOrderByCreatedAtDesc(teamId, InvitationStatus.PENDING)
                .stream()
                .map(this::toResponseWithTeamName)
                .toList();
    }

    // ============================================================
    // Ответ на приглашение
    // ============================================================

    /** Пользователь принимает приглашение. */
    @Transactional
    public InvitationResponse accept(Long invitationId, Long userId) {
        TeamInvitation inv = loadPendingForUser(invitationId, userId);

        // Проверяем, что пользователь не в команде уже (мог вступить другим путём)
        if (teamMemberRepository.existsByTeamIdAndUserIdAndStatus(
                inv.getTeamId(), userId, MemberStatus.ACTIVE)) {
            // На всякий случай — отклоняем приглашение
            inv.setStatus(InvitationStatus.DECLINED);
            inv.setRespondedAt(OffsetDateTime.now());
            invitationRepository.save(inv);
            throw new ConflictException("Вы уже являетесь участником этой команды");
        }

        // Добавляем в команду
        teamMemberService.addMember(inv.getTeamId(), userId, MemberRole.MEMBER);

        inv.setStatus(InvitationStatus.ACCEPTED);
        inv.setRespondedAt(OffsetDateTime.now());
        TeamInvitation saved = invitationRepository.save(inv);

        log.info("Приглашение принято: invitationId={}, userId={}, teamId={}",
                invitationId, userId, inv.getTeamId());

        return toResponseWithTeamName(saved);
    }

    /** Пользователь отклоняет приглашение. */
    @Transactional
    public InvitationResponse decline(Long invitationId, Long userId) {
        TeamInvitation inv = loadPendingForUser(invitationId, userId);

        inv.setStatus(InvitationStatus.DECLINED);
        inv.setRespondedAt(OffsetDateTime.now());
        TeamInvitation saved = invitationRepository.save(inv);

        log.info("Приглашение отклонено: invitationId={}, userId={}", invitationId, userId);
        return toResponseWithTeamName(saved);
    }

    // ============================================================
    // Отмена
    // ============================================================

    /** Владелец/модератор отменяет приглашение. */
    @Transactional
    public InvitationResponse cancel(Long invitationId, Long actorUserId) {
        TeamInvitation inv = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new NotFoundException("Приглашение не найдено"));

        teamMemberService.requireManager(inv.getTeamId(), actorUserId);

        if (inv.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException("Приглашение уже обработано");
        }

        inv.setStatus(InvitationStatus.CANCELLED);
        inv.setRespondedAt(OffsetDateTime.now());
        TeamInvitation saved = invitationRepository.save(inv);

        log.info("Приглашение отменено: invitationId={}, by={}", invitationId, actorUserId);
        return toResponseWithTeamName(saved);
    }

    // ============================================================
    // Helpers
    // ============================================================

    /** Загрузить PENDING-приглашение и проверить, что оно для этого пользователя. */
    private TeamInvitation loadPendingForUser(Long invitationId, Long userId) {
        TeamInvitation inv = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new NotFoundException("Приглашение не найдено"));

        if (!inv.getInvitedUserId().equals(userId)) {
            throw new ForbiddenException("Это приглашение не для вас");
        }

        if (inv.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException("Приглашение уже обработано");
        }

        if (inv.getExpiresAt().isBefore(OffsetDateTime.now())) {
            inv.setStatus(InvitationStatus.EXPIRED);
            inv.setRespondedAt(OffsetDateTime.now());
            invitationRepository.save(inv);
            throw new BadRequestException("Приглашение истекло");
        }

        return inv;
    }

    /** Обновить все просроченные приглашения. */
    @Transactional
    public void expireOverdue() {
        int updated = invitationRepository.expireOverdue(OffsetDateTime.now());
        if (updated > 0) {
            log.debug("Обновлено просроченных приглашений: {}", updated);
        }
    }

    private InvitationResponse toResponseWithTeamName(TeamInvitation inv) {
        String teamName = teamRepository.findById(inv.getTeamId())
                .map(Team::getName)
                .orElse("(удалена)");
        return InvitationResponse.from(inv, teamName);
    }
}