package info.prorabka.abik.server.admin.service;

import info.prorabka.abik.server.common.exception.ForbiddenException;
import info.prorabka.abik.server.user.entity.User;
import info.prorabka.abik.server.user.entity.UserStatus;
import info.prorabka.abik.server.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAccessServiceTest {
    @Mock UserRepository users;
    @InjectMocks AdminAccessService access;

    @Test void superAdminHasAccessToEverything() {
        User user = user(UserStatus.ACTIVE, true, false);
        when(users.findById(1L)).thenReturn(Optional.of(user));
        assertSame(user, access.requireSuperAdmin(1L));
        assertSame(user, access.requireAdManager(1L));
    }

    @Test void adManagerOnlyHasAdvertisingAccess() {
        User user = user(UserStatus.ACTIVE, false, true);
        when(users.findById(2L)).thenReturn(Optional.of(user));
        assertSame(user, access.requireAdManager(2L));
        assertThrows(ForbiddenException.class, () -> access.requireSuperAdmin(2L));
    }

    @Test void regularUserHasNoAdminAccess() {
        when(users.findById(3L)).thenReturn(Optional.of(user(UserStatus.ACTIVE, false, false)));
        assertThrows(ForbiddenException.class, () -> access.requireAdManager(3L));
        assertThrows(ForbiddenException.class, () -> access.requireSuperAdmin(3L));
    }

    @Test void blockedAdminLosesAccessImmediately() {
        when(users.findById(4L)).thenReturn(Optional.of(user(UserStatus.BLOCKED, true, true)));
        assertThrows(ForbiddenException.class, () -> access.requireAdManager(4L));
        assertThrows(ForbiddenException.class, () -> access.requireSuperAdmin(4L));
    }

    private User user(UserStatus status, boolean superAdmin, boolean adAdmin) {
        return User.builder().status(status).isSuperAdmin(superAdmin).isAdAdmin(adAdmin).build();
    }
}
