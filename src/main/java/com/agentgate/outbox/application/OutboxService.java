package com.agentgate.outbox.application;

import com.agentgate.outbox.domain.EventPublication;
import com.agentgate.outbox.infrastructure.EventPublicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxService {

    private final EventPublicationRepository repository;

    public OutboxService(EventPublicationRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public EventPublication publishEvent(String routingKey, String eventType, String payload, String listenerId) {
        EventPublication publication = new EventPublication(
                routingKey,
                eventType,
                payload,
                listenerId != null ? listenerId : "broadcast"
        );
        return repository.save(publication);
    }
}
