package info.prorabka.abik.server.common.security;

import info.prorabka.abik.server.user.entity.User;
import info.prorabka.abik.server.user.entity.UserStatus;
import info.prorabka.abik.server.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    @Mock JwtService jwt;
    @Mock UserRepository users;
    @Mock FilterChain chain;
    @InjectMocks JwtAuthenticationFilter filter;

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void alreadyIssuedTokenIsRejectedAfterBlocking() throws Exception {
        MockHttpServletRequest request = request(); MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwt.isValid("token")).thenReturn(true); when(jwt.parseUserId("token")).thenReturn(8L);
        when(users.findById(8L)).thenReturn(Optional.of(User.builder().status(UserStatus.BLOCKED).build()));
        filter.doFilter(request, response, chain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test void activeUserTokenStillAuthenticates() throws Exception {
        MockHttpServletRequest request = request(); MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwt.isValid("token")).thenReturn(true); when(jwt.parseUserId("token")).thenReturn(9L);
        when(users.findById(9L)).thenReturn(Optional.of(User.builder().status(UserStatus.ACTIVE).build()));
        filter.doFilter(request, response, chain);
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(9L, ((UserPrincipal) authentication.getPrincipal()).userId());
        verify(chain).doFilter(request, response);
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        return request;
    }
}
