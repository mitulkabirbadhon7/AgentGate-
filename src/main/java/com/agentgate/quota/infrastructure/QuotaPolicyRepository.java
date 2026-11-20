package com.agentgate.quota.infrastructure;

import com.agentgate.quota.domain.QuotaPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuotaPolicyRepository extends JpaRepository<QuotaPolicy, Long> {

    @Query("""
        SELECT q FROM QuotaPolicy q
        WHERE (q.scope = 'AGENT_TOOL' AND q.agent.clientId = :agentId AND q.tool.toolId = :toolId)
           OR (q.scope = 'TOOL' AND q.tool.toolId = :toolId)
           OR (q.scope = 'AGENT' AND q.agent.clientId = :agentId)
    """)
    List<QuotaPolicy> findMatchingQuotas(String agentId, Long toolId);
}
