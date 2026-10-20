package com.aipanel.user.web.security;

import com.aipanel.common.constant.CommonConstants;
import com.aipanel.common.domain.R;
import com.aipanel.common.enums.ErrorCode;
import com.aipanel.common.util.JsonUtils;
import com.aipanel.user.core.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Service-side guard for {@code /api/v1/admin/**}.
 * <p>
 * The gateway already rejects non-admin tokens for these paths; this is defense in depth in case
 * the user service is reached without the gateway. It deliberately ignores {@code X-User-ID} and
 * authenticates from the bearer access token itself, then checks {@code t_user.role} in the database.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true; // CORS preflight carries no credentials
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        Long userId = null;
        if (authHeader != null && authHeader.startsWith(CommonConstants.BEARER_PREFIX)) {
            userId = userService.getUserIdFromToken(authHeader.substring(CommonConstants.BEARER_PREFIX.length()));
        }
        if (userId == null) {
            write(response, HttpServletResponse.SC_UNAUTHORIZED, R.fail(ErrorCode.UNAUTHORIZED, "Authentication required"));
            return false;
        }
        if (!userService.isAdmin(userId)) {
            log.warn("Rejected non-admin user {} on {} {}", userId, request.getMethod(), request.getRequestURI());
            write(response, HttpServletResponse.SC_FORBIDDEN, R.fail(ErrorCode.FORBIDDEN, "Admin role required"));
            return false;
        }
        return true;
    }

    private void write(HttpServletResponse response, int status, R<?> body) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String json = JsonUtils.toJson(body);
        response.getWriter().write(json != null ? json : "{}");
    }
}
