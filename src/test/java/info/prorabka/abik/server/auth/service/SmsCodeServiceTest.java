package info.prorabka.abik.server.auth.service;

import info.prorabka.abik.server.admin.service.RegistrationLogService;
import info.prorabka.abik.server.auth.entity.SmsCode;
import info.prorabka.abik.server.auth.entity.SmsPurpose;
import info.prorabka.abik.server.auth.repository.SmsCodeRepository;
import info.prorabka.abik.server.auth.sms.SmsSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsCodeServiceTest {
    @Mock SmsCodeRepository repository;
    @Mock SmsSender sender;
    @Mock RegistrationLogService logs;
    @InjectMocks SmsCodeService service;

    @Test void generationAndGatewayShareRequestId() {
        when(repository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));
        service.generateAndSend("+79990000000", SmsPurpose.REGISTRATION);
        ArgumentCaptor<SmsCode> code = ArgumentCaptor.forClass(SmsCode.class);
        verify(repository).save(code.capture());
        String requestId = code.getValue().getRequestId();
        assertNotNull(requestId);
        verify(sender).send(eq("+79990000000"), org.mockito.ArgumentMatchers.anyString(), eq(SmsPurpose.REGISTRATION), eq(requestId));
        verify(logs).record(eq(requestId), eq("+79990000000"), eq("CODE"), eq("REQUESTED"), eq("REGISTRATION"), org.mockito.ArgumentMatchers.isNull(), eq("SERVER"));
    }

    @Test void verificationUsesGenerationRequestId() {
        SmsCode code = SmsCode.builder().phone("+79990000000").code("1234").purpose(SmsPurpose.REGISTRATION)
                .requestId("trace-123").expiresAt(OffsetDateTime.now().plusMinutes(2)).used(false).attempts((short) 0).build();
        when(repository.findLatestActive("+79990000000", SmsPurpose.REGISTRATION)).thenReturn(Optional.of(code));
        service.verify("+79990000000", SmsPurpose.REGISTRATION, "1234");
        assertTrue(code.getUsed());
        verify(logs).record("trace-123", "+79990000000", "VERIFY", "SUCCESS", "REGISTRATION", null, "SERVER");
    }
}
