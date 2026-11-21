package com.agentgate.sqlite;

import com.agentgate.agent.domain.AgentClient;
import com.agentgate.agent.infrastructure.AgentClientRepository;
import com.agentgate.tool.domain.DownstreamServer;
import com.agentgate.tool.domain.Tool;
import com.agentgate.tool.infrastructure.DownstreamServerRepository;
import com.agentgate.tool.infrastructure.ToolRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:sqlite:target/sqlite-test.db",
    "spring.datasource.driver-class-name=org.sqlite.JDBC",
    "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
class SqliteDatabaseIntegrationTests {

    @Autowired
    private AgentClientRepository agentClientRepository;

    @Autowired
    private DownstreamServerRepository downstreamServerRepository;

    @Autowired
    private ToolRepository toolRepository;

    @Test
    @DisplayName("Should successfully connect to SQLite, create schema via Hibernate, and perform CRUD operations")
    void testSqliteDatabaseCrud() {
        // 1. Create and save AgentClient
        AgentClient agent = new AgentClient("sqlite-test-agent", "SQLite Test Agent", "hashed-secret", "SQLite Agent");
        AgentClient savedAgent = agentClientRepository.save(agent);

        assertThat(savedAgent.getClientId()).isEqualTo("sqlite-test-agent");
        Optional<AgentClient> fetchedAgent = agentClientRepository.findById("sqlite-test-agent");
        assertThat(fetchedAgent).isPresent();
        assertThat(fetchedAgent.get().getName()).isEqualTo("SQLite Test Agent");

        // 2. Create DownstreamServer and Tool
        DownstreamServer server = new DownstreamServer("sqlite-mcp-server", DownstreamServer.ServerType.MCP, "http://localhost:9090", 30, 5);
        DownstreamServer savedServer = downstreamServerRepository.save(server);
        assertThat(savedServer.getServerId()).isNotNull();

        Tool tool = new Tool(
            savedServer,
            "sqlite_query_tool",
            "Queries SQLite DB directly",
            "{\"type\":\"object\"}",
            Tool.RiskLevel.LOW,
            "read"
        );
        Tool savedTool = toolRepository.save(tool);

        assertThat(savedTool.getToolId()).isNotNull();
        Optional<Tool> fetchedTool = toolRepository.findByName("sqlite_query_tool");
        assertThat(fetchedTool).isPresent();
        assertThat(fetchedTool.get().getServer().getName()).isEqualTo("sqlite-mcp-server");
    }
}
