package com.agentgate.audit.infrastructure;

import com.agentgate.audit.domain.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    @Query("SELECT MAX(a.sequenceNumber) FROM AuditEvent a")
    Long findMaxSequenceNumber();

    Optional<AuditEvent> findTopByOrderBySequenceNumberDesc();

    List<AuditEvent> findAllByOrderBySequenceNumberAsc();

    Page<AuditEvent> findByInvocation_Agent_ClientIdOrderBySequenceNumberDesc(String clientId, Pageable pageable);

    Page<AuditEvent> findByEventTypeOrderBySequenceNumberDesc(String eventType, Pageable pageable);
}
