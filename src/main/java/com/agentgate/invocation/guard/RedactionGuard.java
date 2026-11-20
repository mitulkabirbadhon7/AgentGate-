package com.agentgate.invocation.guard;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RedactionGuard {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");

    // Bangladesh mobile numbers: +8801[3-9]XXXXXXXX, 8801[3-9]XXXXXXXX, 01[3-9]XXXXXXXX with optional spaces or dashes
    private static final Pattern BD_PHONE_PATTERN =
            Pattern.compile("(?i)(?:\\+?880[ -]?1|01)[3-9](?:[ -]?\\d){8}\\b");

    // Labeled Bangladesh NIDs: 10, 13, or 17 digits with keywords (nid, national_id, nid_no, etc.)
    private static final Pattern LABELED_NID_PATTERN =
            Pattern.compile("(?i)(\"?(?:nid|national[ _-]?id|nid[ _-]?no|nationalId|nidNumber)\"?\\s*[:=]?\\s*\"?)(\\d{17}|\\d{13}|\\d{10})(\"?)");

    // Standalone 17-digit Bangladesh NID (Year of Birth 19XX or 20XX + 13-digit sequence)
    private static final Pattern STANDALONE_17_NID_PATTERN =
            Pattern.compile("(?<!\\d)(?:19|20)\\d{15}(?!\\d)");

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

        // Redact NID safely: labeled NIDs and standalone 17-digit birth-year prefixed NIDs
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
        // 1. Redact labeled NIDs
        Matcher matcher = LABELED_NID_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1) + "[NID_REDACTED]" + matcher.group(3));
        }
        matcher.appendTail(sb);
        String intermediate = sb.toString();

        // 2. Redact standalone 17-digit NIDs (19XX or 20XX year-prefixed)
        return STANDALONE_17_NID_PATTERN.matcher(intermediate).replaceAll("[NID_REDACTED]");
    }
}
