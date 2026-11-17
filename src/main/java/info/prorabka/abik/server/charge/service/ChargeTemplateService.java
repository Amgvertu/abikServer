package info.prorabka.abik.server.charge.service;

import info.prorabka.abik.server.charge.dto.ChargeTemplateRequest;
import info.prorabka.abik.server.charge.dto.ChargeTemplateResponse;
import info.prorabka.abik.server.charge.entity.*;
import info.prorabka.abik.server.charge.repository.ChargeTemplateRepository;
import info.prorabka.abik.server.common.exception.BadRequestException;
import info.prorabka.abik.server.common.exception.ConflictException;
import info.prorabka.abik.server.common.exception.ForbiddenException;
import info.prorabka.abik.server.common.exception.NotFoundException;
import info.prorabka.abik.server.team.entity.MemberStatus;
import info.prorabka.abik.server.team.repository.TeamMemberRepository;
import info.prorabka.abik.server.team.service.TeamMemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Сервис шаблонов начислений.
 * Управление — владелец/модератор. Шаблоны не удаляются физически — деактивируются.
 */
@Service
public class ChargeTemplateService {

    private static final Logger log = LoggerFactory.getLogger(ChargeTemplateService.class);

    private final ChargeTemplateRepository templateRepository;
    private final ChargeGroupService groupService;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamMemberService teamMemberService;

    public ChargeTemplateService(ChargeTemplateRepository templateRepository,
                                 ChargeGroupService groupService,
                                 TeamMemberRepository teamMemberRepository,
                                 TeamMemberService teamMemberService) {
        this.templateRepository = templateRepository;
        this.groupService = groupService;
        this.teamMemberRepository = teamMemberRepository;
        this.teamMemberService = teamMemberService;
    }

    // ============================================================
    // Чтение
    // ============================================================

    /** Все шаблоны команды. Доступно активным участникам. */
    public List<ChargeTemplateResponse> listTemplates(Long teamId, Long userId, Boolean onlyActive) {
        teamMemberService.requireMember(teamId, userId);

        List<ChargeTemplate> templates = (Boolean.TRUE.equals(onlyActive))
                ? templateRepository.findByTeamIdAndActiveOrderByCreatedAtDesc(teamId, true)
                : templateRepository.findByTeamIdOrderByCreatedAtDesc(teamId);

        return templates.stream()
                .map(ChargeTemplateResponse::from)
                .toList();
    }

    /** Один шаблон. Доступно активным участникам. */
    public ChargeTemplateResponse getTemplate(Long teamId, Long templateId, Long userId) {
        teamMemberService.requireMember(teamId, userId);
        ChargeTemplate t = getTemplateOrThrow(templateId);
        requireTemplateInTeam(t, teamId);
        return ChargeTemplateResponse.from(t);
    }

    // ============================================================
    // Создание
    // ============================================================

    @Transactional
    public ChargeTemplateResponse create(Long teamId, Long userId, ChargeTemplateRequest req) {
        teamMemberService.requireManager(teamId, userId);
        validateRequest(teamId, req);
        if (Boolean.TRUE.equals(req.pollEnabled()) && !Boolean.FALSE.equals(req.active())) {
            requireSingleActivePoll(teamId, null);
        }

        ChargeTemplate template = ChargeTemplate.builder()
                .teamId(teamId)
                .groupId(req.groupId())
                .subgroupId(req.subgroupId())
                .name(req.name().trim())
                .amountKopecks(req.amountKopecks())
                .type(req.type())
                .periodicity(req.periodicity())
                .dayOfMonth(req.dayOfMonth())
                .pollEnabled(Boolean.TRUE.equals(req.pollEnabled()))
                .pollStartDay(req.pollStartDay())
                .pollEndDay(req.pollEndDay())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .distribution(req.distribution())
                .active(req.active() == null ? Boolean.TRUE : req.active())
                .memberIds(req.memberIds() == null ? new HashSet<>() : new HashSet<>(req.memberIds()))
                .build();

        ChargeTemplate saved = templateRepository.save(template);
        log.info("Шаблон создан: teamId={}, templateId={}, name={}", teamId, saved.getId(), saved.getName());
        return ChargeTemplateResponse.from(saved);
    }

    // ============================================================
    // Обновление
    // ============================================================

    @Transactional
    public ChargeTemplateResponse update(Long teamId, Long templateId, Long userId,
                                         ChargeTemplateRequest req) {
        teamMemberService.requireManager(teamId, userId);

        ChargeTemplate template = getTemplateOrThrow(templateId);
        requireTemplateInTeam(template, teamId);
        validateRequest(teamId, req);
        boolean willBeActive = req.active() == null ? Boolean.TRUE.equals(template.getActive()) : req.active();
        if (Boolean.TRUE.equals(req.pollEnabled()) && willBeActive) {
            requireSingleActivePoll(teamId, templateId);
        }

        template.setGroupId(req.groupId());
        template.setSubgroupId(req.subgroupId());
        template.setName(req.name().trim());
        template.setAmountKopecks(req.amountKopecks());
        template.setType(req.type());
        template.setPeriodicity(req.periodicity());
        template.setDayOfMonth(req.dayOfMonth());
        template.setPollEnabled(Boolean.TRUE.equals(req.pollEnabled()));
        template.setPollStartDay(req.pollStartDay());
        template.setPollEndDay(req.pollEndDay());
        template.setStartDate(req.startDate());
        template.setEndDate(req.endDate());
        template.setDistribution(req.distribution());
        if (req.active() != null) {
            template.setActive(req.active());
        }
        template.setMemberIds(req.memberIds() == null ? new HashSet<>() : new HashSet<>(req.memberIds()));

        ChargeTemplate saved = templateRepository.save(template);
        log.info("Шаблон обновлён: templateId={}", templateId);
        return ChargeTemplateResponse.from(saved);
    }

    // ============================================================
    // Активация / деактивация
    // ============================================================

    /**
     * Деактивировать шаблон. Начисления по нему больше не генерируются,
     * но старые — сохраняются. Физически шаблон не удаляется.
     */
    @Transactional
    public ChargeTemplateResponse deactivate(Long teamId, Long templateId, Long userId) {
        teamMemberService.requireManager(teamId, userId);

        ChargeTemplate template = getTemplateOrThrow(templateId);
        requireTemplateInTeam(template, teamId);

        if (Boolean.FALSE.equals(template.getActive())) {
            throw new BadRequestException("Шаблон уже деактивирован");
        }

        template.setActive(false);
        ChargeTemplate saved = templateRepository.save(template);
        log.info("Шаблон деактивирован: templateId={}", templateId);
        return ChargeTemplateResponse.from(saved);
    }

    @Transactional
    public ChargeTemplateResponse activate(Long teamId, Long templateId, Long userId) {
        teamMemberService.requireManager(teamId, userId);

        ChargeTemplate template = getTemplateOrThrow(templateId);
        requireTemplateInTeam(template, teamId);

        if (Boolean.TRUE.equals(template.getActive())) {
            throw new BadRequestException("Шаблон уже активен");
        }
        if (Boolean.TRUE.equals(template.getPollEnabled())) {
            requireSingleActivePoll(teamId, templateId);
        }

        template.setActive(true);
        ChargeTemplate saved = templateRepository.save(template);
        log.info("Шаблон активирован: templateId={}", templateId);
        return ChargeTemplateResponse.from(saved);
    }

    // ============================================================
    // Helpers
    // ============================================================

    /** Получить шаблон или 404. Публичный — понадобится в ChargeService. */
    public ChargeTemplate getTemplateOrThrow(Long templateId) {
        return templateRepository.findById(templateId)
                .orElseThrow(() -> new NotFoundException("Шаблон не найден"));
    }

    private void requireTemplateInTeam(ChargeTemplate t, Long teamId) {
        if (!t.getTeamId().equals(teamId)) {
            throw new ForbiddenException("Шаблон принадлежит другой команде");
        }
    }

    private void requireSingleActivePoll(Long teamId, Long exceptTemplateId) {
        boolean exists = templateRepository.findByTeamIdAndPollEnabledTrueAndActiveTrue(teamId)
                .stream()
                .anyMatch(template -> !template.getId().equals(exceptTemplateId));
        if (exists) {
            throw new ConflictException("В команде уже есть активный шаблон «Абонемент (опрос)»");
        }
    }

    private void validateRequest(Long teamId, ChargeTemplateRequest req) {
        ChargeGroup group = groupService.getGroupOrThrow(req.groupId());
        if (!group.getTeamId().equals(teamId)) {
            throw new ForbiddenException("Группа принадлежит другой команде");
        }
        if (req.subgroupId() != null) {
            ChargeSubgroup subgroup = groupService.getSubgroupOrThrow(req.subgroupId());
            if (!subgroup.getGroupId().equals(group.getId())) {
                throw new BadRequestException("Подгруппа не принадлежит указанной группе");
            }
        }

        boolean poll = Boolean.TRUE.equals(req.pollEnabled());
        Set<Long> memberIds = req.memberIds() == null ? Set.of() : req.memberIds();
        if (!poll) {
            if (memberIds.isEmpty()) {
                throw new BadRequestException("Укажите хотя бы одного участника");
            }
            long activeCount = teamMemberRepository
                    .findByTeamIdAndStatusOrderByJoinedAtAsc(teamId, MemberStatus.ACTIVE)
                    .stream()
                    .filter(m -> memberIds.contains(m.getUserId()))
                    .count();
            if (activeCount != memberIds.size()) {
                throw new BadRequestException(
                        "Среди указанных участников есть те, кто не является активным членом команды");
            }
        }

        if (req.type() == ChargeType.SYSTEMATIC) {
            if (req.periodicity() != Periodicity.MONTHLY) {
                throw new BadRequestException("Систематический шаблон должен быть ежемесячным");
            }
            if (!poll && req.dayOfMonth() == null) {
                throw new BadRequestException("Для систематического шаблона обязателен день месяца");
            }
        } else if (req.type() == ChargeType.ONE_TIME) {
            if (poll) throw new BadRequestException("Абонемент по опросу должен быть ежемесячным");
            if (req.dayOfMonth() != null) {
                throw new BadRequestException("Для разового шаблона день месяца не указывается");
            }
            if (req.periodicity() != Periodicity.ONE_TIME) {
                throw new BadRequestException("Разовый шаблон должен быть с периодичностью ONE_TIME");
            }
        }

        if (poll) {
            if (req.pollStartDay() == null || req.pollEndDay() == null) {
                throw new BadRequestException("Укажите день начала и окончания опроса");
            }
            if (req.pollEndDay() < req.pollStartDay()) {
                throw new BadRequestException("День окончания опроса не может быть меньше дня начала");
            }
            if (req.dayOfMonth() != null) {
                throw new BadRequestException("Для абонемента по опросу день начисления не указывается");
            }
        } else if (req.pollStartDay() != null || req.pollEndDay() != null) {
            throw new BadRequestException("Дни опроса указываются только для абонемента по опросу");
        }

        if (req.startDate() != null && req.endDate() != null
                && req.endDate().isBefore(req.startDate())) {
            throw new BadRequestException("Дата окончания раньше даты начала");
        }
    }
}