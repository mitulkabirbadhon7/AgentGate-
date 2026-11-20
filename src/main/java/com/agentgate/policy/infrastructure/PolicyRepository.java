package com.agentgate.policy.infrastructure;

import com.agentgate.policy.domain.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {
    Optional<Policy> findByName(String name);
    boolean existsByName(String name);
}
