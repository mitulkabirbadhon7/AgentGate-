package com.agentgate.invocation.guard;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RedactionGuard {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");

    // Bangladesh mobile numbers: +8801[3-9]XXXXXXXX, 8801[3-9]XXXXXXXX, or 01[3-9]XXXXXXXX
    private static final Pattern BD_PHONE_PATTERN =
            Pattern.compile("(\\+?8801|01)[3-9]\\d{8}\\b");

    // Bangladesh NID: 10 digit (smart NID), 13 or 17 digit (legacy NID)
    private static final Pattern NID_PATTERN =
            Pattern.compile("\\b(\\d{17}|\\d{13}|\\d{10})\\b");

    private final int maxResponseLength;

    public RedactionGuard(
            @Value("${agentgate.governance.max-response-length:32768}") int maxResponseLength) {
        this.maxResponseLength = maxResponseLength;
    }

    public String redactSensitiveData(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        String result = EMAIL_PATTERN.matcher(input).replaceAll("[EMAIL_REDACTED]");
        result = BD_PHONE_PATTERN.matcher(result).replaceAll("[BD_PHONE_REDACTED]");

        // Redact NID only if explicitly in NID context or standalone long ID
        result = redactNidSafely(result);

        return result;
    }

    public String enforceTruncation(String input) {
        if (input == null) return null;
        if (input.length() > maxResponseLength) {
            return input.substring(0, maxResponseLength)
                    + "\n... [TRUNCATED by AgentGate: Output exceeded maximum allowed limit of "
                    + maxResponseLength + " characters]";
        }
        return input;
    }

    private String redactNidSafely(String input) {
        // Look for keywords indicating NID context, or direct numbers labeled as nid (longest first for proper regex alternation)
        Matcher matcher = Pattern.compile("(?i)(\"nid\"\\s*:\\s*\"?)(\\d{17}|\\d{13}|\\d{10})(\"?)").matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1) + "[NID_REDACTED]" + matcher.group(3));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
