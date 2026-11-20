package com.agentgate.approval.infrastructure;

import com.agentgate.approval.domain.ApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {
    Optional<ApprovalRequest> findByInvocation_InvocationId(Long invocationId);

    List<ApprovalRequest> findByStatusOrderByCreatedAtDesc(ApprovalRequest.ApprovalStatus status);

    @Query("SELECT ar FROM ApprovalRequest ar WHERE ar.status = 'PENDING' AND ar.invocation.agent.clientId = :agentId")
    List<ApprovalRequest> findPendingByAgentId(String agentId);

    @Query("SELECT ar FROM ApprovalRequest ar WHERE ar.status = 'PENDING' AND ar.invocation.tool.toolId = :toolId")
    List<ApprovalRequest> findPendingByToolId(Long toolId);

    @Query("SELECT ar FROM ApprovalRequest ar WHERE ar.status = 'PENDING' AND ar.expiresAt <= :now")
    List<ApprovalRequest> findExpiredRequests(Instant now);
}
