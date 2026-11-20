package com.agentgate.security.token;

import com.agentgate.security.principal.SecurityContextSnapshot;
import com.agentgate.shared.api.ErrorCode;
import com.agentgate.shared.exception.AgentGateException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
public class TokenService {

    private final String expectedIssuer;
    private final String expectedAudience;
    private final byte[] secretKeyBytes;
    private final ObjectMapper objectMapper;

    public TokenService(
            @Value("${agentgate.security.jwt.issuer:https://auth.agentgate.local}") String expectedIssuer,
            @Value("${agentgate.security.jwt.audience:agentgate-core}") String expectedAudience,
            @Value("${agentgate.security.jwt.secret-key:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef}") String secretKey,
            ObjectMapper objectMapper) {
        this.expectedIssuer = expectedIssuer;
        this.expectedAudience = expectedAudience;
        this.secretKeyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        this.objectMapper = objectMapper;
    }

    public String generateToken(String agentId, String userId, Set<String> scopes, long ttlSeconds) {
        try {
            Map<String, Object> header = Map.of(
                    "alg", "HS256",
                    "typ", "JWT"
            );

            long now = Instant.now().getEpochSecond();
            Map<String, Object> claims = new HashMap<>();
            claims.put("iss", expectedIssuer);
            claims.put("aud", expectedAudience);
            claims.put("sub", agentId);
            claims.put("iat", now);
            claims.put("exp", now + ttlSeconds);
            if (userId != null && !userId.isBlank()) {
                claims.put("obo", userId);
            }
            if (scopes != null && !scopes.isEmpty()) {
                claims.put("scope", String.join(" ", scopes));
            }

            String encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(header));
            String encodedClaims = Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(claims));
            String signatureInput = encodedHeader + "." + encodedClaims;

            byte[] signature = signHmacSha256(signatureInput.getBytes(StandardCharsets.UTF_8), secretKeyBytes);
            String encodedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(signature);

            return signatureInput + "." + encodedSignature;
        } catch (Exception e) {
            throw new AgentGateException(ErrorCode.INTERNAL_ERROR, "Failed to generate JWT token: " + e.getMessage(), e);
        }
    }

    public SecurityContextSnapshot validateAndParse(String token) {
        if (token == null || token.isBlank()) {
            throw new AgentGateException(ErrorCode.UNAUTHORIZED, "Authorization token is missing");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new AgentGateException(ErrorCode.UNAUTHORIZED, "Malformed JWT structure");
        }

        String signatureInput = parts[0] + "." + parts[1];
        byte[] expectedSig = signHmacSha256(signatureInput.getBytes(StandardCharsets.UTF_8), secretKeyBytes);
        byte[] actualSig;
        try {
            actualSig = Base64.getUrlDecoder().decode(parts[2]);
        } catch (IllegalArgumentException e) {
            throw new AgentGateException(ErrorCode.UNAUTHORIZED, "Invalid JWT signature encoding");
        }

        if (!MessageDigest.isEqual(expectedSig, actualSig)) {
            throw new AgentGateException(ErrorCode.UNAUTHORIZED, "Invalid JWT signature");
        }

        try {
            byte[] claimsBytes = Base64.getUrlDecoder().decode(parts[1]);
            Map<String, Object> claims = objectMapper.readValue(claimsBytes, new TypeReference<Map<String, Object>>() {});

            // Validate Expiry
            Object expObj = claims.get("exp");
            if (expObj instanceof Number expNum) {
                long exp = expNum.longValue();
                if (Instant.now().getEpochSecond() >= exp) {
                    throw new AgentGateException(ErrorCode.UNAUTHORIZED, "Token has expired");
                }
            } else {
                throw new AgentGateException(ErrorCode.UNAUTHORIZED, "Token missing expiration claim");
            }

            // Validate Issuer
            String issuer = (String) claims.get("iss");
            if (!expectedIssuer.equals(issuer)) {
                throw new AgentGateException(ErrorCode.UNAUTHORIZED, "Invalid token issuer: " + issuer);
            }

            // Validate Audience
            String audience = (String) claims.get("aud");
            if (!expectedAudience.equals(audience)) {
                throw new AgentGateException(ErrorCode.UNAUTHORIZED, "Invalid token audience: " + audience);
            }

            // Extract Agent ID (sub)
            String agentId = (String) claims.get("sub");
            if (agentId == null || agentId.isBlank()) {
                throw new AgentGateException(ErrorCode.UNAUTHORIZED, "Token missing subject (agent_id)");
            }

            // Extract On-Behalf-Of User ID (obo)
            String userId = (String) claims.get("obo");

            // Extract Scopes
            Set<String> scopes = new HashSet<>();
            Object scopeObj = claims.get("scope");
            if (scopeObj instanceof String scopeStr) {
                scopes.addAll(Arrays.asList(scopeStr.split("\\s+")));
            }

            String clearance = (String) claims.getOrDefault("clearance", "STANDARD");

            return new SecurityContextSnapshot(agentId, userId, clearance, scopes);
        } catch (AgentGateException e) {
            throw e;
        } catch (Exception e) {
            throw new AgentGateException(ErrorCode.UNAUTHORIZED, "Failed to parse token claims: " + e.getMessage(), e);
        }
    }

    private byte[] signHmacSha256(byte[] data, byte[] key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA256", e);
        }
    }
}
