package info.prorabka.abik.server.charge.service;

import info.prorabka.abik.server.charge.dto.ChargeGroupRequest;
import info.prorabka.abik.server.charge.dto.ChargeGroupResponse;
import info.prorabka.abik.server.charge.dto.ChargeSubgroupRequest;
import info.prorabka.abik.server.charge.dto.ChargeSubgroupResponse;
import info.prorabka.abik.server.charge.entity.ChargeGroup;
import info.prorabka.abik.server.charge.entity.ChargeSubgroup;
import info.prorabka.abik.server.charge.repository.ChargeGroupRepository;
import info.prorabka.abik.server.charge.repository.ChargeSubgroupRepository;
import info.prorabka.abik.server.common.exception.ConflictException;
import info.prorabka.abik.server.common.exception.ForbiddenException;
import info.prorabka.abik.server.common.exception.NotFoundException;
import info.prorabka.abik.server.team.service.TeamMemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Сервис групп и подгрупп начислений.
 * Управление доступно владельцу и модератору команды.
 */
@Service
public class ChargeGroupService {

    private static final Logger log = LoggerFactory.getLogger(ChargeGroupService.class);

    private final ChargeGroupRepository groupRepository;
    private final ChargeSubgroupRepository subgroupRepository;
    private final TeamMemberService teamMemberService;

    public ChargeGroupService(ChargeGroupRepository groupRepository,
                              ChargeSubgroupRepository subgroupRepository,
                              TeamMemberService teamMemberService) {
        this.groupRepository = groupRepository;
        this.subgroupRepository = subgroupRepository;
        this.teamMemberService = teamMemberService;
    }

    // ============================================================
    // Группы
    // ============================================================

    /** Список групп команды. Доступно активным участникам. */
    public List<ChargeGroupResponse> listGroups(Long teamId, Long userId) {
        teamMemberService.requireMember(teamId, userId);
        return groupRepository.findByTeamIdOrderByNameAsc(teamId).stream()
                .map(ChargeGroupResponse::from)
                .toList();
    }

    @Transactional
    public ChargeGroupResponse createGroup(Long teamId, Long userId, ChargeGroupRequest req) {
        teamMemberService.requireManager(teamId, userId);

        String name = req.name().trim();
        if (groupRepository.existsByTeamIdAndName(teamId, name)) {
            throw new ConflictException("Группа с таким названием уже существует в команде");
        }

        ChargeGroup group = ChargeGroup.builder()
                .teamId(teamId)
                .name(name)
                .build();
        ChargeGroup saved = groupRepository.save(group);
        log.info("Группа создана: teamId={}, groupId={}, name={}", teamId, saved.getId(), name);
        return ChargeGroupResponse.from(saved);
    }

    @Transactional
    public ChargeGroupResponse renameGroup(Long teamId, Long groupId, Long userId, ChargeGroupRequest req) {
        teamMemberService.requireManager(teamId, userId);

        ChargeGroup group = getGroupOrThrow(groupId);
        requireGroupInTeam(group, teamId);

        String name = req.name().trim();
        groupRepository.findByTeamIdAndName(teamId, name).ifPresent(existing -> {
            if (!existing.getId().equals(groupId)) {
                throw new ConflictException("Группа с таким названием уже существует");
            }
        });

        group.setName(name);
        ChargeGroup saved = groupRepository.save(group);
        log.info("Группа переименована: groupId={}, name={}", groupId, name);
        return ChargeGroupResponse.from(saved);
    }

    @Transactional
    public void deleteGroup(Long teamId, Long groupId, Long userId) {
        teamMemberService.requireManager(teamId, userId);

        ChargeGroup group = getGroupOrThrow(groupId);
        requireGroupInTeam(group, teamId);

        // В БД FK с ON DELETE RESTRICT, поэтому при наличии шаблонов
        // удаление упадёт с SQL-ошибкой. Дадим понятное сообщение.
        try {
            groupRepository.delete(group);
            groupRepository.flush();
        } catch (Exception e) {
            throw new ConflictException(
                    "Нельзя удалить группу: к ней привязаны шаблоны или начисления. " +
                            "Сначала удалите или перенесите их.");
        }
        log.info("Группа удалена: groupId={}", groupId);
    }

    // ============================================================
    // Подгруппы
    // ============================================================

    /** Список подгрупп группы. Доступно активным участникам команды. */
    public List<ChargeSubgroupResponse> listSubgroups(Long teamId, Long groupId, Long userId) {
        teamMemberService.requireMember(teamId, userId);
        ChargeGroup group = getGroupOrThrow(groupId);
        requireGroupInTeam(group, teamId);

        return subgroupRepository.findByGroupIdOrderByNameAsc(groupId).stream()
                .map(ChargeSubgroupResponse::from)
                .toList();
    }

    @Transactional
    public ChargeSubgroupResponse createSubgroup(Long teamId, Long groupId, Long userId,
                                                 ChargeSubgroupRequest req) {
        teamMemberService.requireManager(teamId, userId);

        ChargeGroup group = getGroupOrThrow(groupId);
        requireGroupInTeam(group, teamId);

        String name = req.name().trim();
        if (subgroupRepository.existsByGroupIdAndName(groupId, name)) {
            throw new ConflictException("Подгруппа с таким названием уже существует");
        }

        ChargeSubgroup subgroup = ChargeSubgroup.builder()
                .groupId(groupId)
                .name(name)
                .build();
        ChargeSubgroup saved = subgroupRepository.save(subgroup);
        log.info("Подгруппа создана: groupId={}, subgroupId={}, name={}", groupId, saved.getId(), name);
        return ChargeSubgroupResponse.from(saved);
    }

    @Transactional
    public ChargeSubgroupResponse renameSubgroup(Long teamId, Long subgroupId, Long userId,
                                                 ChargeSubgroupRequest req) {
        teamMemberService.requireManager(teamId, userId);

        ChargeSubgroup subgroup = getSubgroupOrThrow(subgroupId);
        ChargeGroup group = getGroupOrThrow(subgroup.getGroupId());
        requireGroupInTeam(group, teamId);

        String name = req.name().trim();
        subgroupRepository.findByGroupIdAndName(subgroup.getGroupId(), name).ifPresent(existing -> {
            if (!existing.getId().equals(subgroupId)) {
                throw new ConflictException("Подгруппа с таким названием уже существует");
            }
        });

        subgroup.setName(name);
        ChargeSubgroup saved = subgroupRepository.save(subgroup);
        log.info("Подгруппа переименована: subgroupId={}, name={}", subgroupId, name);
        return ChargeSubgroupResponse.from(saved);
    }

    @Transactional
    public void deleteSubgroup(Long teamId, Long subgroupId, Long userId) {
        teamMemberService.requireManager(teamId, userId);

        ChargeSubgroup subgroup = getSubgroupOrThrow(subgroupId);
        ChargeGroup group = getGroupOrThrow(subgroup.getGroupId());
        requireGroupInTeam(group, teamId);

        try {
            subgroupRepository.delete(subgroup);
            subgroupRepository.flush();
        } catch (Exception e) {
            throw new ConflictException(
                    "Нельзя удалить подгруппу: к ней привязаны шаблоны или начисления.");
        }
        log.info("Подгруппа удалена: subgroupId={}", subgroupId);
    }

    // ============================================================
    // Helpers
    // ============================================================

    /** Получить группу или 404. */
    public ChargeGroup getGroupOrThrow(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Группа не найдена"));
    }

    /** Получить подгруппу или 404. */
    public ChargeSubgroup getSubgroupOrThrow(Long subgroupId) {
        return subgroupRepository.findById(subgroupId)
                .orElseThrow(() -> new NotFoundException("Подгруппа не найдена"));
    }

    /** Проверить, что группа принадлежит команде. */
    private void requireGroupInTeam(ChargeGroup group, Long teamId) {
        if (!group.getTeamId().equals(teamId)) {
            throw new ForbiddenException("Группа принадлежит другой команде");
        }
    }
}