package com.aipanel.user.web.security;

import com.aipanel.user.core.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminAuthInterceptorTest {

    private final UserService userService = mock(UserService.class);
    private final AdminAuthInterceptor interceptor = new AdminAuthInterceptor(userService);

    private static MockHttpServletRequest request(String bearer) {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/admin/orders");
        if (bearer != null) {
            req.addHeader("Authorization", "Bearer " + bearer);
        }
        return req;
    }

    @Test
    void missingTokenIs401EvenWithSpoofedUserHeader() throws Exception {
        MockHttpServletRequest req = request(null);
        req.addHeader("X-User-ID", "1");
        MockHttpServletResponse res = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(req, res, new Object())).isFalse();
        assertThat(res.getStatus()).isEqualTo(401);
        verify(userService, never()).isAdmin(1L);
    }

    @Test
    void invalidTokenIs401() throws Exception {
        when(userService.getUserIdFromToken("bad")).thenReturn(null);
        MockHttpServletResponse res = new MockHttpServletResponse();
        assertThat(interceptor.preHandle(request("bad"), res, new Object())).isFalse();
        assertThat(res.getStatus()).isEqualTo(401);
    }

    @Test
    void nonAdminIs403() throws Exception {
        when(userService.getUserIdFromToken("user-token")).thenReturn(7L);
        when(userService.isAdmin(7L)).thenReturn(false);
        MockHttpServletResponse res = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request("user-token"), res, new Object())).isFalse();
        assertThat(res.getStatus()).isEqualTo(403);
        assertThat(res.getContentAsString()).contains("Admin role required");
    }

    @Test
    void adminPasses() throws Exception {
        when(userService.getUserIdFromToken("admin-token")).thenReturn(1L);
        when(userService.isAdmin(1L)).thenReturn(true);
        MockHttpServletResponse res = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request("admin-token"), res, new Object())).isTrue();
        assertThat(res.getStatus()).isEqualTo(200);
    }

    @Test
    void preflightPasses() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("OPTIONS", "/api/v1/admin/orders");
        assertThat(interceptor.preHandle(req, new MockHttpServletResponse(), new Object())).isTrue();
    }
}
