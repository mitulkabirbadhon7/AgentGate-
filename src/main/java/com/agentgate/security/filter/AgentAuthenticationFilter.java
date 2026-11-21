package com.agentgate.security.filter;

import com.agentgate.security.principal.SecurityContextSnapshot;
import com.agentgate.security.token.TokenService;
import com.agentgate.shared.api.ApiError;
import com.agentgate.shared.api.ErrorCode;
import com.agentgate.shared.exception.AgentGateException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AgentAuthenticationFilter extends OncePerRequestFilter {

    public static final String SECURITY_CONTEXT_ATTR = "AGENTGATE_SECURITY_CONTEXT";
    private final TokenService tokenService;
    private final ObjectMapper objectMapper;

    public AgentAuthenticationFilter(TokenService tokenService, ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 1. Defend against agent identity spoofing
        if (request.getHeader("X-Agent-Id") != null || request.getHeader("X-Agent-ID") != null) {
            writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, ErrorCode.FORBIDDEN,
                    "Direct client identity headers (X-Agent-Id) are prohibited. Identity must be derived from verified OAuth2 bearer token.");
            return;
        }

        // 2. Untrusted on-behalf-of user header rejection
        if (request.getHeader("X-User-Id") != null || request.getHeader("X-User-ID") != null) {
            writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, ErrorCode.FORBIDDEN,
                    "Untrusted caller user headers (X-User-Id) are prohibited. On-behalf-of user identity must be asserted via verified token claims.");
            return;
        }

        // Bypass public endpoints
        if (isPublicEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            try {
                SecurityContextSnapshot snapshot = tokenService.validateAndParse(token);
                request.setAttribute(SECURITY_CONTEXT_ATTR, snapshot);

                List<SimpleGrantedAuthority> authorities = snapshot.scopes().stream()
                        .map(s -> new SimpleGrantedAuthority("SCOPE_" + s))
                        .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(snapshot.agentId(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (AgentGateException ex) {
                writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, ex.getErrorCode(), ex.getMessage());
                return;
            } catch (Exception ex) {
                writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Invalid authentication token");
                return;
            }
        } else if (requiresAuthentication(path)) {
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Missing Bearer authorization header");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/actuator") ||
               path.startsWith("/api/v1/auth") ||
               path.startsWith("/api/v1/mock") ||
               path.startsWith("/css") ||
               path.startsWith("/js") ||
               path.startsWith("/images") ||
               path.equals("/favicon.ico");
    }

    private boolean requiresAuthentication(String path) {
        return path.startsWith("/mcp") || path.startsWith("/api/");
    }

    private void writeErrorResponse(HttpServletResponse response, int status, ErrorCode code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError error = ApiError.of(code, message, null);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
