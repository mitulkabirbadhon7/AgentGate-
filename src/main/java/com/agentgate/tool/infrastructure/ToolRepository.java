package com.agentgate.tool.infrastructure;

import com.agentgate.tool.domain.Tool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ToolRepository extends JpaRepository<Tool, Long> {
    Optional<Tool> findByName(String name);
    boolean existsByName(String name);
    List<Tool> findByStatus(Tool.ToolStatus status);

    @Query("SELECT t FROM Tool t WHERE t.status = 'ACTIVE'")
    List<Tool> findAllActiveTools();
}
