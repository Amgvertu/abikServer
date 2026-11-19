package info.prorabka.abik.server.charge.service;

import info.prorabka.abik.server.charge.dto.ChargeTemplateRequest;
import info.prorabka.abik.server.charge.dto.ChargeTemplateResponse;
import info.prorabka.abik.server.charge.entity.ChargeDistribution;
import info.prorabka.abik.server.charge.entity.ChargeGroup;
import info.prorabka.abik.server.charge.entity.ChargeTemplate;
import info.prorabka.abik.server.charge.entity.ChargeType;
import info.prorabka.abik.server.charge.entity.Periodicity;
import info.prorabka.abik.server.charge.repository.ChargeTemplateRepository;
import info.prorabka.abik.server.common.exception.BadRequestException;
import info.prorabka.abik.server.team.repository.TeamMemberRepository;
import info.prorabka.abik.server.team.service.TeamMemberService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChargeTemplateServiceTest {

    @Mock ChargeTemplateRepository templateRepository;
    @Mock ChargeGroupService groupService;
    @Mock TeamMemberRepository teamMemberRepository;
    @Mock TeamMemberService teamMemberService;

    @InjectMocks ChargeTemplateService service;

    @Test
    void createsPollSubscriptionWithoutChargeDayAndPredefinedMembers() {
        ChargeTemplateRequest request = pollRequest((short) 1, (short) 31);
        when(groupService.getGroupOrThrow(20L)).thenReturn(
                ChargeGroup.builder().id(20L).teamId(1L).name("Абонемент").build());
        when(templateRepository.findByTeamIdAndPollEnabledTrueAndActiveTrue(1L)).thenReturn(List.of());
        when(templateRepository.save(any(ChargeTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChargeTemplateResponse response = service.create(1L, 7L, request);

        ArgumentCaptor<ChargeTemplate> captor = ArgumentCaptor.forClass(ChargeTemplate.class);
        verify(templateRepository).save(captor.capture());
        ChargeTemplate saved = captor.getValue();
        assertTrue(saved.getPollEnabled());
        assertEquals((short) 1, saved.getPollStartDay());
        assertEquals((short) 31, saved.getPollEndDay());
        assertNull(saved.getDayOfMonth());
        assertTrue(saved.getMemberIds().isEmpty());
        assertTrue(saved.getActive());
        assertTrue(response.pollEnabled());
        assertEquals(150_000L, response.amountKopecks());
    }

    @Test
    void rejectsPollWithEndDayBeforeStartDay() {
        ChargeTemplateRequest request = pollRequest((short) 25, (short) 20);
        when(groupService.getGroupOrThrow(20L)).thenReturn(
                ChargeGroup.builder().id(20L).teamId(1L).name("Абонемент").build());

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> service.create(1L, 7L, request));

        assertEquals("День окончания опроса не может быть меньше дня начала", error.getMessage());
        verify(templateRepository, never()).save(any());
    }

    private ChargeTemplateRequest pollRequest(short startDay, short endDay) {
        return new ChargeTemplateRequest(
                20L,
                null,
                "Абонемент (опрос)",
                150_000L,
                ChargeType.SYSTEMATIC,
                Periodicity.MONTHLY,
                null,
                true,
                startDay,
                endDay,
                null,
                null,
                ChargeDistribution.FIXED,
                Set.of(),
                null
        );
    }
}
