package com.agentgate.resilience;

import com.agentgate.invocation.guard.RedactionGuard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedactionGuardTests {

    private final RedactionGuard redactionGuard = new RedactionGuard(100);

    @Test
    @DisplayName("Email redaction: masks valid email addresses and preserves innocent text")
    void testEmailRedaction() {
        String input = "Please contact support at alice.smith@gateway.io or security@company.org for help.";
        String redacted = redactionGuard.redactSensitiveData(input);

        assertThat(redacted).doesNotContain("alice.smith@gateway.io");
        assertThat(redacted).doesNotContain("security@company.org");
        assertThat(redacted).contains("[EMAIL_REDACTED]");
        assertThat(redacted).contains("Please contact support at");
    }

    @Test
    @DisplayName("Bangladesh phone number redaction: masks +8801, 8801, and 01 prefixes with optional spaces or dashes")
    void testBangladeshPhoneRedaction() {
        String input = "Call +8801712345678 or 01899887766 or backup 8801955443322 or formatted +880 1712 345678 or 01712-345678 for dispatch.";
        String redacted = redactionGuard.redactSensitiveData(input);

        assertThat(redacted).doesNotContain("+8801712345678");
        assertThat(redacted).doesNotContain("01899887766");
        assertThat(redacted).doesNotContain("8801955443322");
        assertThat(redacted).doesNotContain("+880 1712 345678");
        assertThat(redacted).doesNotContain("01712-345678");
        assertThat(redacted).contains("[BD_PHONE_REDACTED]");
    }

    @Test
    @DisplayName("Bangladesh NID redaction: masks 10, 13, and 17 digit NID in JSON and labeled text")
    void testNidRedaction() {
        String input = "{\"applicant\": \"Rahim\", \"nid\": \"19901234567890123\", \"national_id\": \"1234567890\", \"status\": \"ACTIVE\"} and text National ID: 19951234567890123";
        String redacted = redactionGuard.redactSensitiveData(input);

        assertThat(redacted).doesNotContain("19901234567890123");
        assertThat(redacted).doesNotContain("1234567890");
        assertThat(redacted).doesNotContain("19951234567890123");
        assertThat(redacted).contains("\"nid\": \"[NID_REDACTED]\"");
        assertThat(redacted).contains("\"national_id\": \"[NID_REDACTED]\"");
        assertThat(redacted).contains("National ID: [NID_REDACTED]");
        assertThat(redacted).contains("\"applicant\": \"Rahim\"");
    }

    @Test
    @DisplayName("Truncation notice: appends notice when response exceeds max allowed characters")
    void testTruncationNotice() {
        String longText = "A".repeat(150);
        String truncated = redactionGuard.enforceTruncation(longText);

        assertThat(truncated.length()).isGreaterThan(100);
        assertThat(truncated).startsWith("A".repeat(100));
        assertThat(truncated).contains("[TRUNCATED by AgentGate: Output exceeded maximum allowed limit of 100 characters]");
    }

    @Test
    @DisplayName("Truncation notice: untouched when within max characters")
    void testWithinLimitNoTruncation() {
        String shortText = "Within limit response";
        String result = redactionGuard.enforceTruncation(shortText);

        assertThat(result).isEqualTo(shortText);
        assertThat(result).doesNotContain("TRUNCATED");
    }
}
