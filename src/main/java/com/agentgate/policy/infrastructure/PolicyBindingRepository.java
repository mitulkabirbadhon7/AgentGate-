package com.agentgate.policy.infrastructure;

import com.agentgate.policy.domain.PolicyBinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyBindingRepository extends JpaRepository<PolicyBinding, Long> {

    @Query("""
        SELECT pb FROM PolicyBinding pb
        WHERE (pb.scope = 'AGENT_TOOL' AND pb.agent.clientId = :agentId AND pb.tool.toolId = :toolId)
           OR (pb.scope = 'TOOL' AND pb.tool.toolId = :toolId)
           OR (pb.scope = 'AGENT' AND pb.agent.clientId = :agentId)
           OR (pb.scope = 'GLOBAL')
        ORDER BY
           CASE pb.scope
               WHEN 'AGENT_TOOL' THEN 1
               WHEN 'TOOL' THEN 2
               WHEN 'AGENT' THEN 3
               WHEN 'GLOBAL' THEN 4
               ELSE 5
           END ASC,
           pb.priority ASC
    """)
    List<PolicyBinding> findApplicableBindings(String agentId, Long toolId);

    List<PolicyBinding> findByPolicy_PolicyId(Long policyId);
}
