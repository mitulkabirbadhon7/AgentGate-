package com.agentgate.audit.application;

import com.agentgate.audit.domain.AuditEvent;
import com.agentgate.audit.infrastructure.AuditEventRepository;
import com.agentgate.invocation.domain.ToolInvocation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

@Service
public class AuditChainService {

    private static final Logger log = LoggerFactory.getLogger(AuditChainService.class);
    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper canonicalObjectMapper;

    public AuditChainService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
        this.canonicalObjectMapper = new ObjectMapper();
        this.canonicalObjectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public synchronized AuditEvent appendAuditEvent(ToolInvocation invocation, String eventType, String rawPayload, String actor) {
        String canonicalPayload = toCanonicalJson(rawPayload);

        AuditEvent latestEvent = auditEventRepository.findTopByOrderBySequenceNumberDesc().orElse(null);
        long sequenceNumber = (latestEvent != null) ? latestEvent.getSequenceNumber() + 1 : 1L;
        String prevHash = (latestEvent != null) ? latestEvent.getCurrHash() : AuditEvent.GENESIS_HASH;

        String currHash = computeChainHash(prevHash, sequenceNumber, canonicalPayload);

        AuditEvent event = new AuditEvent(
                invocation,
                sequenceNumber,
                eventType,
                canonicalPayload,
                prevHash,
                currHash,
                actor != null ? actor : "system"
        );

        return auditEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public AuditDTOs.AuditVerificationReport verifyAuditIntegrity() {
        List<AuditEvent> chain = auditEventRepository.findAllByOrderBySequenceNumberAsc();
        if (chain.isEmpty()) {
            return AuditDTOs.AuditVerificationReport.valid(0);
        }

        String expectedPrevHash = AuditEvent.GENESIS_HASH;
        long expectedSeq = 1L;

        for (AuditEvent event : chain) {
            // Check sequence gap
            if (!event.getSequenceNumber().equals(expectedSeq)) {
                return AuditDTOs.AuditVerificationReport.tampered(
                        chain.size(), event.getSequenceNumber(), "Sequence gap detected. Expected: " + expectedSeq + ", Found: " + event.getSequenceNumber()
                );
            }

            // Check linkage to previous hash
            if (!event.getPrevHash().equalsIgnoreCase(expectedPrevHash)) {
                return AuditDTOs.AuditVerificationReport.tampered(
                        chain.size(), event.getSequenceNumber(), "Broken chain link. PrevHash does not match preceding CurrHash."
                );
            }

            // Verify recalculation of curr_hash
            String canonicalPayload = toCanonicalJson(event.getPayload());
            String recalculatedHash = computeChainHash(event.getPrevHash(), event.getSequenceNumber(), canonicalPayload);
            if (!event.getCurrHash().equalsIgnoreCase(recalculatedHash)) {
                return AuditDTOs.AuditVerificationReport.tampered(
                        chain.size(), event.getSequenceNumber(), "Hash mismatch! Payload or hash was modified."
                );
            }

            expectedPrevHash = event.getCurrHash();
            expectedSeq++;
        }

        return AuditDTOs.AuditVerificationReport.valid(chain.size());
    }

    @Transactional(readOnly = true)
    public Page<AuditDTOs.AuditEventResponse> listAuditEvents(Pageable pageable) {
        return auditEventRepository.findAll(pageable)
                .map(AuditDTOs.AuditEventResponse::from);
    }

    @Transactional(readOnly = true)
    public List<AuditDTOs.AuditEventResponse> exportAllEvents() {
        return auditEventRepository.findAllByOrderBySequenceNumberAsc().stream()
                .map(AuditDTOs.AuditEventResponse::from)
                .toList();
    }

    public String computeChainHash(String prevHash, long sequenceNumber, String canonicalPayload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = prevHash + ":" + sequenceNumber + ":" + canonicalPayload;
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate SHA-256 chain hash", e);
        }
    }

    private String toCanonicalJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return "{}";
        }
        try {
            JsonNode tree = canonicalObjectMapper.readTree(raw);
            return canonicalObjectMapper.writeValueAsString(tree);
        } catch (Exception e) {
            return "{\"raw\":\"" + raw.replace("\"", "\\\"") + "\"}";
        }
    }
}
