package com.agentgate.invocation.infrastructure;

import com.agentgate.invocation.domain.ToolInvocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ToolInvocationRepository extends JpaRepository<ToolInvocation, Long> {
    Optional<ToolInvocation> findByEventId(UUID eventId);
    boolean existsByEventId(UUID eventId);
    Page<ToolInvocation> findByAgent_ClientId(String clientId, Pageable pageable);
    Page<ToolInvocation> findByStatus(ToolInvocation.InvocationStatus status, Pageable pageable);
}
