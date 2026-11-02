package info.prorabka.abik.server.charge.service;

import info.prorabka.abik.server.charge.dto.ChargeListRequest;
import info.prorabka.abik.server.charge.dto.ChargeListResponse;
import info.prorabka.abik.server.charge.entity.ChargeList;
import info.prorabka.abik.server.charge.repository.ChargeListRepository;
import info.prorabka.abik.server.common.exception.BadRequestException;
import info.prorabka.abik.server.common.exception.ForbiddenException;
import info.prorabka.abik.server.common.exception.NotFoundException;
import info.prorabka.abik.server.team.entity.MemberStatus;
import info.prorabka.abik.server.team.entity.TeamMember;
import info.prorabka.abik.server.team.repository.TeamMemberRepository;
import info.prorabka.abik.server.team.service.TeamMemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Именованные списки получателей начислений.
 * Чтение — любой участник команды, изменение — управляющие.
 */
@Service
public class ChargeListService {

    private final ChargeListRepository listRepository;
    private final TeamMemberService teamMemberService;
    private final TeamMemberRepository teamMemberRepository;

    public ChargeListService(ChargeListRepository listRepository,
                             TeamMemberService teamMemberService,
                             TeamMemberRepository teamMemberRepository) {
        this.listRepository = listRepository;
        this.teamMemberService = teamMemberService;
        this.teamMemberRepository = teamMemberRepository;
    }

    public List<ChargeListResponse> list(Long teamId, Long actor) {
        teamMemberService.requireMember(teamId, actor);
        return listRepository.findByTeamIdOrderByNameAsc(teamId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ChargeListResponse create(Long teamId, Long actor, ChargeListRequest req) {
        teamMemberService.requireManager(teamId, actor);
        Set<Long> memberIds = validateMembers(teamId, req.memberIds());
        ChargeList saved = listRepository.save(ChargeList.builder()
                .teamId(teamId)
                .name(req.name().trim())
                .memberIds(memberIds)
                .build());
        return toResponse(saved);
    }

    @Transactional
    public ChargeListResponse update(Long teamId, Long listId, Long actor, ChargeListRequest req) {
        teamMemberService.requireManager(teamId, actor);
        ChargeList list = getListOrThrow(teamId, listId);
        list.setName(req.name().trim());
        list.setMemberIds(validateMembers(teamId, req.memberIds()));
        return toResponse(listRepository.save(list));
    }

    @Transactional
    public void delete(Long teamId, Long listId, Long actor) {
        teamMemberService.requireManager(teamId, actor);
        ChargeList list = getListOrThrow(teamId, listId);
        listRepository.delete(list);
    }

    private ChargeList getListOrThrow(Long teamId, Long listId) {
        ChargeList list = listRepository.findById(listId)
                .orElseThrow(() -> new NotFoundException("Список не найден"));
        if (!list.getTeamId().equals(teamId)) {
            throw new ForbiddenException("Список принадлежит другой команде");
        }
        return list;
    }

    /** Все участники списка должны быть активными членами команды. */
    private Set<Long> validateMembers(Long teamId, Set<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            throw new BadRequestException("Выберите хотя бы одного участника");
        }
        Set<Long> activeIds = teamMemberRepository
                .findByTeamIdAndStatusOrderByJoinedAtAsc(teamId, MemberStatus.ACTIVE)
                .stream().map(TeamMember::getUserId).collect(Collectors.toSet());
        for (Long uid : memberIds) {
            if (!activeIds.contains(uid)) {
                throw new BadRequestException(
                        "Участник не является активным членом команды: userId=" + uid);
            }
        }
        return new HashSet<>(memberIds);
    }

    private ChargeListResponse toResponse(ChargeList list) {
        return new ChargeListResponse(list.getId(), list.getName(), Set.copyOf(list.getMemberIds()));
    }
}
