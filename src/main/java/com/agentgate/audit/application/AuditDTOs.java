package com.agentgate.audit.application;

import com.agentgate.audit.domain.AuditEvent;

import java.time.Instant;

public class AuditDTOs {

    public record AuditEventResponse(
        Long auditId,
        Long invocationId,
        Long sequenceNumber,
        String eventType,
        Instant eventTime,
        String payload,
        String prevHash,
        String currHash,
        String actor
    ) {
        public static AuditEventResponse from(AuditEvent event) {
            return new AuditEventResponse(
                event.getAuditId(),
                event.getInvocation().getInvocationId(),
                event.getSequenceNumber(),
                event.getEventType(),
                event.getEventTime(),
                event.getPayload(),
                event.getPrevHash(),
                event.getCurrHash(),
                event.getActor()
            );
        }
    }

    public record AuditVerificationReport(
        boolean valid,
        long eventsChecked,
        Long brokenAtSequence,
        String message
    ) {
        public static AuditVerificationReport valid(long count) {
            return new AuditVerificationReport(true, count, null, "Audit cryptographic hash chain integrity fully verified.");
        }

        public static AuditVerificationReport tampered(long count, long brokenSeq, String reason) {
            return new AuditVerificationReport(false, count, brokenSeq, "TAMPER DETECTED at sequence " + brokenSeq + ": " + reason);
        }
    }
}
