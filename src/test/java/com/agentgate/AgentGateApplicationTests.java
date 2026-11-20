package com.agentgate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AgentGateApplicationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoadsAndFlywayBaselineExecutes() {
        // Verify Flyway baseline migration created essential tables
        Integer tableCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE LOWER(table_name) IN ('agent_client', 'tool', 'policy', 'tool_invocation', 'audit_event')",
            Integer.class
        );
        assertThat(tableCount).isNotNull().isGreaterThanOrEqualTo(5);
    }
}
