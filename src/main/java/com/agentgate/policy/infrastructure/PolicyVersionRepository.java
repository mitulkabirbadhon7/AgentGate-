package com.agentgate.policy.infrastructure;

import com.agentgate.policy.domain.PolicyVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyVersionRepository extends JpaRepository<PolicyVersion, Long> {
    Optional<PolicyVersion> findByPolicy_PolicyIdAndVersionNumber(Long policyId, int versionNumber);

    @Query("SELECT MAX(pv.versionNumber) FROM PolicyVersion pv WHERE pv.policy.policyId = :policyId")
    Integer findMaxVersionNumber(Long policyId);

    List<PolicyVersion> findByPolicy_PolicyIdOrderByVersionNumberDesc(Long policyId);
}
