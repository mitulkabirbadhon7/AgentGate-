package com.agentgate.tool.infrastructure;

import com.agentgate.tool.domain.DownstreamServer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DownstreamServerRepository extends JpaRepository<DownstreamServer, Long> {
    Optional<DownstreamServer> findByName(String name);
    boolean existsByName(String name);
}
