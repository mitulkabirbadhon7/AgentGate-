package com.agentgate.alert.infrastructure;

import com.agentgate.alert.domain.SecurityAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SecurityAlertRepository extends JpaRepository<SecurityAlert, Long> {
    Page<SecurityAlert> findByOrderByCreatedAtDesc(Pageable pageable);
    List<SecurityAlert> findTop20ByOrderByCreatedAtDesc();
}
