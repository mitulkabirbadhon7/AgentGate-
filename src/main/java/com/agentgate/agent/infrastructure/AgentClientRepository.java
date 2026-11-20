package com.agentgate.agent.infrastructure;

import com.agentgate.agent.domain.AgentClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgentClientRepository extends JpaRepository<AgentClient, String> {
    Optional<AgentClient> findByName(String name);
    boolean existsByName(String name);
}
