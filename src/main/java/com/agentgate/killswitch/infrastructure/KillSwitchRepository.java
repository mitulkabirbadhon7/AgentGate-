package com.agentgate.killswitch.infrastructure;

import com.agentgate.killswitch.domain.KillSwitch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KillSwitchRepository extends JpaRepository<KillSwitch, Long> {

    @Query("""
        SELECT ks FROM KillSwitch ks
        WHERE ks.status = 'ACTIVE'
          AND (ks.scope = 'GLOBAL_WRITES'
           OR (ks.scope = 'AGENT' AND ks.targetId = :agentId)
           OR (ks.scope = 'TOOL' AND ks.targetId = :toolId))
    """)
    List<KillSwitch> findActiveMatchingSwitches(String agentId, String toolId);

    List<KillSwitch> findByStatus(KillSwitch.KillSwitchStatus status);
}
