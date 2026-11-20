-- AgentGate V1 Baseline Schema
-- Authoritative schema definitions for Zero-Trust Governance Gateway

-- 1. AGENT_CLIENT
CREATE TABLE IF NOT EXISTS agent_client (
    client_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    client_secret_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(128),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. END_USER
CREATE TABLE IF NOT EXISTS end_user (
    user_id VARCHAR(64) PRIMARY KEY,
    external_ref VARCHAR(128) NOT NULL,
    issuer VARCHAR(255) NOT NULL,
    clearance_level VARCHAR(32) NOT NULL DEFAULT 'STANDARD',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_end_user_issuer_ref UNIQUE (issuer, external_ref)
);

-- 3. DOWNSTREAM_SERVER
CREATE TABLE IF NOT EXISTS downstream_server (
    server_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(128) NOT NULL UNIQUE,
    type VARCHAR(32) NOT NULL,
    endpoint_url VARCHAR(512) NOT NULL,
    health_check_interval_sec INT NOT NULL DEFAULT 30,
    circuit_open_threshold INT NOT NULL DEFAULT 5,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. TOOL
CREATE TABLE IF NOT EXISTS tool (
    tool_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    server_id BIGINT NOT NULL REFERENCES downstream_server(server_id) ON DELETE CASCADE,
    name VARCHAR(128) NOT NULL UNIQUE,
    description TEXT,
    input_schema TEXT NOT NULL,
    risk_level VARCHAR(32) NOT NULL DEFAULT 'LOW',
    routing_hint VARCHAR(128),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. POLICY
CREATE TABLE IF NOT EXISTS policy (
    policy_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(128) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    current_version_id BIGINT,
    active_version_locks BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. POLICY_VERSION (Immutable)
CREATE TABLE IF NOT EXISTS policy_version (
    version_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    policy_id BIGINT NOT NULL REFERENCES policy(policy_id) ON DELETE CASCADE,
    version_number INT NOT NULL,
    rules_tree TEXT NOT NULL,
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    audit_by_user VARCHAR(128) NOT NULL,
    version_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_policy_version UNIQUE (policy_id, version_number)
);

-- 7. POLICY_BINDING
CREATE TABLE IF NOT EXISTS policy_binding (
    binding_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    policy_id BIGINT NOT NULL REFERENCES policy(policy_id) ON DELETE CASCADE,
    agent_id VARCHAR(64) REFERENCES agent_client(client_id) ON DELETE CASCADE,
    tool_id BIGINT REFERENCES tool(tool_id) ON DELETE CASCADE,
    scope VARCHAR(32) NOT NULL,
    priority INT NOT NULL DEFAULT 100,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 8. KILL_SWITCH
CREATE TABLE IF NOT EXISTS kill_switch (
    kill_switch_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    scope VARCHAR(32) NOT NULL,
    target_id VARCHAR(128),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    reason TEXT NOT NULL,
    admin_id VARCHAR(128) NOT NULL,
    activated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deactivated_at TIMESTAMP WITH TIME ZONE
);

-- 9. QUOTA_POLICY
CREATE TABLE IF NOT EXISTS quota_policy (
    quota_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    scope VARCHAR(32) NOT NULL,
    agent_id VARCHAR(64) REFERENCES agent_client(client_id) ON DELETE CASCADE,
    tool_id BIGINT REFERENCES tool(tool_id) ON DELETE CASCADE,
    max_amount BIGINT NOT NULL,
    window_unit VARCHAR(32) NOT NULL DEFAULT 'DAILY_UTC',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 10. RATE_LIMIT_POLICY
CREATE TABLE IF NOT EXISTS rate_limit_policy (
    rate_limit_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    scope VARCHAR(32) NOT NULL,
    agent_id VARCHAR(64) REFERENCES agent_client(client_id) ON DELETE CASCADE,
    tool_id BIGINT REFERENCES tool(tool_id) ON DELETE CASCADE,
    window_seconds INT NOT NULL DEFAULT 60,
    max_requests INT NOT NULL DEFAULT 100,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 11. TOOL_INVOCATION
CREATE TABLE IF NOT EXISTS tool_invocation (
    invocation_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    agent_id VARCHAR(64) NOT NULL REFERENCES agent_client(client_id),
    user_id VARCHAR(64) REFERENCES end_user(user_id),
    tool_id BIGINT NOT NULL REFERENCES tool(tool_id),
    request_arguments_redacted TEXT NOT NULL,
    response_payload_redacted TEXT,
    status VARCHAR(32) NOT NULL,
    error_code VARCHAR(64),
    policy_version_id BIGINT REFERENCES policy_version(version_id),
    latency_ms INT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE
);

-- 12. AUDIT_EVENT (Append-Only)
CREATE TABLE IF NOT EXISTS audit_event (
    audit_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    invocation_id BIGINT NOT NULL REFERENCES tool_invocation(invocation_id),
    sequence_number BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    event_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payload TEXT NOT NULL,
    prev_hash VARCHAR(64) NOT NULL,
    curr_hash VARCHAR(64) NOT NULL,
    hash_algorithm VARCHAR(32) NOT NULL DEFAULT 'SHA-256',
    actor VARCHAR(128) NOT NULL
);

-- 13. APPROVAL_REQUEST
CREATE TABLE IF NOT EXISTS approval_request (
    approval_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    invocation_id BIGINT NOT NULL UNIQUE REFERENCES tool_invocation(invocation_id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    original_arguments_encrypted TEXT NOT NULL,
    edited_arguments_encrypted TEXT,
    approver_id VARCHAR(128),
    decision_reason TEXT,
    arguments_hash VARCHAR(64) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 14. SECURITY_ALERT
CREATE TABLE IF NOT EXISTS security_alert (
    alert_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    alert_type VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    triggered_by VARCHAR(64) REFERENCES agent_client(client_id),
    event_id VARCHAR(128),
    details TEXT NOT NULL,
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 15. EVENT_PUBLICATION (Transactional Outbox)
CREATE TABLE IF NOT EXISTS event_publication (
    id UUID PRIMARY KEY,
    event_routing_key VARCHAR(255) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    event_payload TEXT NOT NULL,
    listener_id VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    last_error TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance & security invariants
CREATE INDEX IF NOT EXISTS idx_tool_invocation_agent ON tool_invocation(agent_id);
CREATE INDEX IF NOT EXISTS idx_tool_invocation_tool ON tool_invocation(tool_id);
CREATE INDEX IF NOT EXISTS idx_tool_invocation_status ON tool_invocation(status);
CREATE INDEX IF NOT EXISTS idx_audit_event_invocation ON audit_event(invocation_id);
CREATE INDEX IF NOT EXISTS idx_audit_event_seq ON audit_event(sequence_number);
CREATE INDEX IF NOT EXISTS idx_approval_status ON approval_request(status);
CREATE INDEX IF NOT EXISTS idx_kill_switch_scope ON kill_switch(scope, status);
CREATE INDEX IF NOT EXISTS idx_policy_binding_lookup ON policy_binding(scope, agent_id, tool_id);
CREATE INDEX IF NOT EXISTS idx_outbox_status ON event_publication(status, next_attempt_at);
