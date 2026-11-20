package com.agentgate.security;

import com.agentgate.security.principal.SecurityContextSnapshot;
import com.agentgate.security.token.TokenService;
import com.agentgate.shared.api.ErrorCode;
import com.agentgate.shared.exception.AgentGateException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenServiceTests {

    private TokenService tokenService;
    private final String secret = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(
                "https://auth.agentgate.local",
                "agentgate-core",
                secret,
                new ObjectMapper()
        );
    }

    @Test
    void validToken_successfullyParsesSnapshot() {
        String token = tokenService.generateToken("agent-sales-01", "usr-alice", Set.of("tools:read", "tools:call"), 3600);
        SecurityContextSnapshot snapshot = tokenService.validateAndParse(token);

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.agentId()).isEqualTo("agent-sales-01");
        assertThat(snapshot.userId()).isEqualTo("usr-alice");
        assertThat(snapshot.hasScope("tools:read")).isTrue();
        assertThat(snapshot.hasScope("tools:call")).isTrue();
        assertThat(snapshot.hasScope("admin")).isFalse();
    }

    @Test
    void expiredToken_throwsUnauthorized() throws InterruptedException {
        // Generate token with 0 second TTL
        String token = tokenService.generateToken("agent-sales-01", "usr-alice", Set.of("tools:read"), -5);

        assertThatThrownBy(() -> tokenService.validateAndParse(token))
                .isInstanceOf(AgentGateException.class)
                .satisfies(ex -> {
                    AgentGateException age = (AgentGateException) ex;
                    assertThat(age.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
                    assertThat(age.getMessage()).contains("expired");
                });
    }

    @Test
    void wrongAudience_throwsUnauthorized() {
        TokenService wrongAudienceService = new TokenService(
                "https://auth.agentgate.local",
                "wrong-audience",
                secret,
                new ObjectMapper()
        );
        String token = wrongAudienceService.generateToken("agent-sales-01", null, Set.of(), 3600);

        assertThatThrownBy(() -> tokenService.validateAndParse(token))
                .isInstanceOf(AgentGateException.class)
                .satisfies(ex -> {
                    AgentGateException age = (AgentGateException) ex;
                    assertThat(age.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
                    assertThat(age.getMessage()).contains("audience");
                });
    }

    @Test
    void tamperedSignature_throwsUnauthorized() {
        String token = tokenService.generateToken("agent-sales-01", null, Set.of(), 3600);
        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";

        assertThatThrownBy(() -> tokenService.validateAndParse(tamperedToken))
                .isInstanceOf(AgentGateException.class)
                .satisfies(ex -> {
                    AgentGateException age = (AgentGateException) ex;
                    assertThat(age.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
                });
    }
}
