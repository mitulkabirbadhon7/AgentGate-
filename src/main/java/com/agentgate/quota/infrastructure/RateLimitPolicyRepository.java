package com.agentgate.quota.infrastructure;

import com.agentgate.quota.domain.RateLimitPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RateLimitPolicyRepository extends JpaRepository<RateLimitPolicy, Long> {

    @Query("""
        SELECT p FROM RateLimitPolicy p
        WHERE (p.scope = 'AGENT_TOOL' AND p.agent.clientId = :agentId AND p.tool.toolId = :toolId)
           OR (p.scope = 'TOOL' AND p.tool.toolId = :toolId)
           OR (p.scope = 'AGENT' AND p.agent.clientId = :agentId)
    """)
    List<RateLimitPolicy> findMatchingPolicies(String agentId, Long toolId);
}
