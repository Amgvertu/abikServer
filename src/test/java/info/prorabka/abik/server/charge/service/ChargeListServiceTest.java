package info.prorabka.abik.server.charge.service;

import info.prorabka.abik.server.charge.dto.ChargeListRequest;
import info.prorabka.abik.server.charge.dto.ChargeListResponse;
import info.prorabka.abik.server.charge.entity.ChargeList;
import info.prorabka.abik.server.charge.repository.ChargeListRepository;
import info.prorabka.abik.server.common.exception.BadRequestException;
import info.prorabka.abik.server.event.service.EventParticipationService;
import info.prorabka.abik.server.team.entity.MemberStatus;
import info.prorabka.abik.server.team.entity.TeamMember;
import info.prorabka.abik.server.team.repository.TeamMemberRepository;
import info.prorabka.abik.server.team.service.TeamMemberService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChargeListServiceTest {

    @Mock ChargeListRepository listRepository;
    @Mock TeamMemberService teamMemberService;
    @Mock TeamMemberRepository teamMemberRepository;
    @Mock EventParticipationService participationService;
    @InjectMocks ChargeListService service;

    @Test
    void updateDropsLegacyInactiveIdsAndCleansFutureEvents() {
        ChargeList list = ChargeList.builder().id(5L).teamId(1L).name("Состав")
                .memberIds(new HashSet<>(Set.of(100L, 200L))).build();
        when(listRepository.findById(5L)).thenReturn(Optional.of(list));
        when(listRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(teamMemberRepository.findByTeamIdAndStatusOrderByJoinedAtAsc(1L, MemberStatus.ACTIVE))
                .thenReturn(List.of(member(200L), member(300L)));

        ChargeListResponse response = service.update(1L, 5L, 900L,
                new ChargeListRequest("Состав", Set.of(100L, 200L, 300L)));

        assertEquals(Set.of(200L, 300L), response.memberIds());
        verify(participationService).removeFromList(5L, 100L);
    }

    @Test
    void updateRejectsNewInactiveId() {
        ChargeList list = ChargeList.builder().id(5L).teamId(1L).name("Состав")
                .memberIds(new HashSet<>(Set.of(200L))).build();
        when(listRepository.findById(5L)).thenReturn(Optional.of(list));
        when(teamMemberRepository.findByTeamIdAndStatusOrderByJoinedAtAsc(1L, MemberStatus.ACTIVE))
                .thenReturn(List.of(member(200L)));

        assertThrows(BadRequestException.class, () -> service.update(1L, 5L, 900L,
                new ChargeListRequest("Состав", Set.of(200L, 999L))));
    }

    private TeamMember member(Long userId) {
        return TeamMember.builder().teamId(1L).userId(userId).status(MemberStatus.ACTIVE).build();
    }
}
