package com.agentgate.bootstrap;

import com.agentgate.agent.domain.AgentClient;
import com.agentgate.agent.infrastructure.AgentClientRepository;
import com.agentgate.policy.domain.Policy;
import com.agentgate.policy.domain.PolicyBinding;
import com.agentgate.policy.domain.PolicyVersion;
import com.agentgate.policy.infrastructure.PolicyBindingRepository;
import com.agentgate.policy.infrastructure.PolicyRepository;
import com.agentgate.policy.infrastructure.PolicyVersionRepository;
import com.agentgate.tool.domain.DownstreamServer;
import com.agentgate.tool.domain.Tool;
import com.agentgate.tool.infrastructure.DownstreamServerRepository;
import com.agentgate.tool.infrastructure.ToolRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!test")
public class DataBootstrapInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataBootstrapInitializer.class);

    private final AgentClientRepository agentRepository;
    private final DownstreamServerRepository serverRepository;
    private final ToolRepository toolRepository;
    private final PolicyRepository policyRepository;
    private final PolicyVersionRepository policyVersionRepository;
    private final PolicyBindingRepository policyBindingRepository;
    private final PasswordEncoder passwordEncoder;

    public DataBootstrapInitializer(
            AgentClientRepository agentRepository,
            DownstreamServerRepository serverRepository,
            ToolRepository toolRepository,
            PolicyRepository policyRepository,
            PolicyVersionRepository policyVersionRepository,
            PolicyBindingRepository policyBindingRepository,
            PasswordEncoder passwordEncoder) {
        this.agentRepository = agentRepository;
        this.serverRepository = serverRepository;
        this.toolRepository = toolRepository;
        this.policyRepository = policyRepository;
        this.policyVersionRepository = policyVersionRepository;
        this.policyBindingRepository = policyBindingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        bootstrapAgents();
        bootstrapTools();
        bootstrapPolicies();
    }

    private void bootstrapAgents() {
        if (!agentRepository.existsById("demo-agent")) {
            log.info("Bootstrapping initial AgentGate demo agent client...");
            AgentClient demoAgent = new AgentClient(
                    "demo-agent",
                    "Demo Governance Agent",
                    passwordEncoder.encode("demo-secret"),
                    "AgentGate Reference AI Agent"
            );
            agentRepository.save(demoAgent);
            log.info("Bootstrapped demo agent: 'demo-agent' (Secret: 'demo-secret')");
        }

        if (!agentRepository.existsById("mcp-agent-default")) {
            log.info("Bootstrapping default MCP agent bridge client...");
            AgentClient mcpAgent = new AgentClient(
                    "mcp-agent-default",
                    "Default MCP Agent Client",
                    passwordEncoder.encode("mcp-default-secret"),
                    "AgentGate MCP Protocol Bridge"
            );
            agentRepository.save(mcpAgent);
            log.info("Bootstrapped default MCP agent: 'mcp-agent-default'");
        }
    }

    private void bootstrapTools() {
        if (!serverRepository.existsByName("local-mock-service")) {
            log.info("Bootstrapping initial downstream services and governed tools...");
            DownstreamServer mockServer = new DownstreamServer(
                    "local-mock-service",
                    DownstreamServer.ServerType.REST,
                    "http://localhost:8080/api/v1/mock",
                    30,
                    5
            );
            DownstreamServer savedServer = serverRepository.save(mockServer);

            if (!toolRepository.existsByName("weather_service")) {
                Tool weatherTool = new Tool(
                        savedServer,
                        "weather_service",
                        "Governed real-time meteorological query tool for global cities",
                        "{\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\"}},\"required\":[\"city\"]}",
                        Tool.RiskLevel.LOW,
                        "POST /weather"
                );
                toolRepository.save(weatherTool);
            }

            if (!toolRepository.existsByName("echo_tool")) {
                Tool echoTool = new Tool(
                        savedServer,
                        "echo_tool",
                        "Governed payload inspection and echo diagnostic tool",
                        "{\"type\":\"object\",\"properties\":{\"message\":{\"type\":\"string\"}}}",
                        Tool.RiskLevel.LOW,
                        "POST /echo"
                );
                toolRepository.save(echoTool);
            }

            log.info("Bootstrapped tools: 'weather_service', 'echo_tool'");
        }
    }

    private long downstreamServerRepositoryCount() {
        return serverRepository.count();
    }

    private void bootstrapPolicies() {
        if (policyRepository.count() == 0) {
            log.info("Bootstrapping initial baseline governance policy...");
            Policy baselinePolicy = new Policy("baseline-governance-policy");
            Policy savedPolicy = policyRepository.save(baselinePolicy);

            String rulesTree = """
                    {
                      "rules": [
                        {
                          "id": "rule-allow-baseline",
                          "effect": "ALLOW",
                          "reason": "Baseline Zero-Trust policy permitting governed tools"
                        }
                      ]
                    }
                    """;

            PolicyVersion version = new PolicyVersion(
                    savedPolicy,
                    1,
                    rulesTree,
                    "system-bootstrap",
                    "sha256-baseline-v1-hash"
            );
            PolicyVersion savedVersion = policyVersionRepository.save(version);

            savedPolicy.setCurrentVersionId(savedVersion.getVersionId());
            policyRepository.save(savedPolicy);

            AgentClient demoAgent = agentRepository.findById("demo-agent").orElse(null);
            if (demoAgent != null) {
                PolicyBinding agentBinding = new PolicyBinding(
                        savedPolicy,
                        demoAgent,
                        null,
                        PolicyBinding.BindingScope.AGENT,
                        100
                );
                policyBindingRepository.save(agentBinding);
                log.info("Bootstrapped baseline policy: 'baseline-governance-policy' (v1, AGENT: demo-agent)");
            }

            AgentClient mcpAgent = agentRepository.findById("mcp-agent-default").orElse(null);
            if (mcpAgent != null) {
                PolicyBinding mcpBinding = new PolicyBinding(
                        savedPolicy,
                        mcpAgent,
                        null,
                        PolicyBinding.BindingScope.AGENT,
                        100
                );
                policyBindingRepository.save(mcpBinding);
                log.info("Bootstrapped baseline policy binding for MCP agent: 'mcp-agent-default'");
            }
        }
    }
}
