package com.agentgate.outbox.infrastructure;

import com.agentgate.outbox.domain.EventPublication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventPublicationRepository extends JpaRepository<EventPublication, UUID> {
    List<EventPublication> findByStatusAndNextAttemptAtLessThanEqual(
            EventPublication.OutboxStatus status, Instant time);
}
