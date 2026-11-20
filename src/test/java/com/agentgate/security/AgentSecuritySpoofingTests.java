package com.agentgate.security;

import com.agentgate.security.filter.AgentAuthenticationFilter;
import com.agentgate.security.token.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AgentSecuritySpoofingTests {

    private AgentAuthenticationFilter filter;
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(
                "https://auth.agentgate.local",
                "agentgate-core",
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                new ObjectMapper()
        );
        filter = new AgentAuthenticationFilter(tokenService, new ObjectMapper());
    }

    @Test
    void whenCallerSuppliesXAgentId_filterRejectsWith403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
        request.addHeader("X-Agent-Id", "rogue-agent");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("X-Agent-Id");
    }

    @Test
    void whenCallerSuppliesXUserId_filterRejectsWith403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
        request.addHeader("X-User-Id", "victim-user");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("X-User-Id");
    }

    @Test
    void whenValidBearerTokenProvided_filterAllowsAndSetsContext() throws Exception {
        String token = tokenService.generateToken("trusted-agent", "trusted-user", Set.of("call"), 3600);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(request.getAttribute(AgentAuthenticationFilter.SECURITY_CONTEXT_ATTR)).isNotNull();
    }
}
