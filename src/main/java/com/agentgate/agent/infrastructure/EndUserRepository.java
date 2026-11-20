package com.agentgate.agent.infrastructure;

import com.agentgate.agent.domain.EndUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EndUserRepository extends JpaRepository<EndUser, String> {
    Optional<EndUser> findByIssuerAndExternalRef(String issuer, String externalRef);
}
