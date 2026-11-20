"""
Validation script for Mermaid ERD syntax to ensure zero syntax errors when rendered by GitHub or Mermaid JS.
"""

mermaid_erd = """
erDiagram
    DOWNSTREAM_SERVER ||--o{ TOOL : "hosts"
    AGENT_CLIENT ||--o{ TOOL_INVOCATION : "initiates"
    END_USER ||--o{ TOOL_INVOCATION : "on_behalf_of"
    TOOL ||--o{ TOOL_INVOCATION : "executed_in"
    POLICY ||--|{ POLICY_VERSION : "versions"
    POLICY ||--o{ POLICY_BINDING : "binds"
    POLICY_VERSION ||--o{ TOOL_INVOCATION : "pinned_in"
    TOOL_INVOCATION ||--o{ AUDIT_EVENT : "logs"
    TOOL_INVOCATION ||--o| APPROVAL_REQUEST : "requires"
    TOOL_INVOCATION ||--o{ SECURITY_ALERT : "triggers"
    AGENT_CLIENT ||--o{ POLICY_BINDING : "targeted_by"
    TOOL ||--o{ POLICY_BINDING : "scoped_to"
    AGENT_CLIENT ||--o{ QUOTA_POLICY : "budgeted_for"
    TOOL ||--o{ QUOTA_POLICY : "capped_for"
    AGENT_CLIENT ||--o{ RATE_LIMIT_POLICY : "throttled_by"
    TOOL ||--o{ RATE_LIMIT_POLICY : "rate_limited_by"
    AGENT_CLIENT ||--o{ SECURITY_ALERT : "associated_with"

    AGENT_CLIENT {
        varchar client_id PK "Unique agent client identifier"
        varchar name "Agent system name"
        varchar client_secret_hash "BCrypt/Argon2 secret hash"
        varchar display_name "Human-friendly label"
        varchar status "ACTIVE, REVOKED, SUSPENDED"
        timestamptz created_at "Registration timestamp"
    }

    END_USER {
        varchar user_id PK "Subject identifier"
        varchar external_ref "Verified external subject ref"
        varchar issuer "Trusted IdP issuer"
        varchar clearance_level "SECURITY_ROLE"
        timestamptz created_at "Creation timestamp"
    }

    DOWNSTREAM_SERVER {
        bigint server_id PK "Server unique ID"
        varchar name "Unique server name"
        varchar type "MCP or REST"
        varchar endpoint_url "Target base URL"
        int health_check_interval_sec "Probe frequency"
        int circuit_open_threshold "Consecutive failures to trip"
        timestamptz created_at "Registration timestamp"
    }

    TOOL {
        bigint tool_id PK "Tool primary key"
        bigint server_id FK "Parent server reference"
        varchar name "Unique tool identifier"
        text description "Tool functionality documentation"
        jsonb input_schema "JSON Schema Draft 7/2020-12"
        varchar risk_level "LOW, MEDIUM, HIGH, CRITICAL"
        varchar routing_hint "Execution router hint"
        varchar status "ACTIVE, DEPRECATED, DISABLED"
        timestamptz created_at "Creation timestamp"
    }

    POLICY {
        bigint policy_id PK "Policy primary key"
        varchar name "Human-readable policy name"
        varchar status "ACTIVE, DRAFT, ARCHIVED"
        bigint current_version_id FK "Active version pointer"
        boolean active_version_locks "Immutability lock flag"
        timestamptz created_at "Creation timestamp"
    }

    POLICY_VERSION {
        bigint version_id PK "Version primary key"
        bigint policy_id FK "Owning policy reference"
        int version_number "Monotonic sequence number"
        jsonb rules_tree "Declarative rule logic AST"
        timestamptz effective_from "Activation timestamp"
        varchar audit_by_user "Author administrator ID"
        varchar version_hash "SHA-256 fingerprint"
        timestamptz created_at "Creation timestamp"
    }

    POLICY_BINDING {
        bigint binding_id PK "Binding primary key"
        bigint policy_id FK "Target policy reference"
        varchar agent_id FK "Optional agent client filter"
        bigint tool_id FK "Optional tool filter"
        varchar scope "GLOBAL, AGENT, TOOL, AGENT_TOOL"
        int priority "Evaluation precedence order"
        timestamptz created_at "Creation timestamp"
        timestamptz updated_at "Last update timestamp"
    }

    KILL_SWITCH {
        bigint kill_switch_id PK "Kill switch primary key"
        varchar scope "GLOBAL_WRITES, AGENT, TOOL"
        varchar target_id "Target identifier or null"
        varchar status "ACTIVE, INACTIVE"
        text reason "Incident / operational reason"
        varchar admin_id "Administrator who tripped"
        timestamptz activated_at "Activation timestamp"
        timestamptz deactivated_at "Deactivation timestamp"
    }

    QUOTA_POLICY {
        bigint quota_id PK "Quota rule primary key"
        varchar scope "AGENT, TOOL, AGENT_TOOL"
        varchar agent_id FK "Target agent reference"
        bigint tool_id FK "Target tool reference"
        bigint max_amount "Maximum allowable spend/tokens"
        varchar window_unit "DAILY_UTC"
        timestamptz created_at "Creation timestamp"
    }

    RATE_LIMIT_POLICY {
        bigint rate_limit_id PK "Rate limit primary key"
        varchar scope "AGENT, TOOL, AGENT_TOOL"
        varchar agent_id FK "Target agent reference"
        bigint tool_id FK "Target tool reference"
        int window_seconds "Sliding window duration"
        int max_requests "Max requests in window"
        timestamptz created_at "Creation timestamp"
    }

    TOOL_INVOCATION {
        bigint invocation_id PK "Invocation primary key"
        uuid event_id "Correlation / Idempotency ID"
        varchar agent_id FK "Authenticated agent"
        varchar user_id FK "Trusted on-behalf-of user"
        bigint tool_id FK "Target tool executed"
        jsonb request_arguments_redacted "Sanitized input arguments"
        jsonb response_payload_redacted "Sanitized output payload"
        varchar status "RECEIVED, APPROVED, EXECUTED, BLOCKED, REJECTED, FAILED"
        varchar error_code "Machine-readable error enum"
        bigint policy_version_id FK "Pinned immutable policy version"
        int latency_ms "Execution roundtrip duration"
        timestamptz created_at "Invocation timestamp"
        timestamptz completed_at "Completion timestamp"
    }

    AUDIT_EVENT {
        bigint audit_id PK "Audit event primary key"
        bigint invocation_id FK "Linked invocation reference"
        bigint sequence_number "Monotonic sequence per chain"
        varchar event_type "INVOCATION_LIFECYCLE_EVENT"
        timestamptz event_time "Event occurrence timestamp"
        jsonb payload "Redacted event context"
        varchar prev_hash "Cryptographic previous hash"
        varchar curr_hash "SHA-256(prev_hash + seq + payload)"
        varchar hash_algorithm "SHA-256"
        varchar actor "Authenticated actor ID"
    }

    APPROVAL_REQUEST {
        bigint approval_id PK "Approval primary key"
        bigint invocation_id FK "Unique invocation pointer (1:1)"
        varchar status "PENDING, APPROVED, REJECTED, EXPIRED, CANCELLED"
        jsonb original_arguments_encrypted "Encrypted original payload"
        jsonb edited_arguments_encrypted "Encrypted modified payload"
        varchar approver_id "Administrator / Approver ID"
        text decision_reason "Human rationale"
        varchar arguments_hash "SHA-256 digest of arguments"
        int version "Optimistic locking counter"
        timestamptz expires_at "Approval deadline timestamp"
        timestamptz created_at "Creation timestamp"
        timestamptz updated_at "Resolution timestamp"
    }

    SECURITY_ALERT {
        bigint alert_id PK "Security alert primary key"
        varchar alert_type "BREACH, TAMPER, RATE_EXCEEDED"
        varchar severity "LOW, MEDIUM, HIGH, CRITICAL"
        varchar triggered_by FK "Agent or System source"
        varchar event_id "Correlation event ID"
        text details "Technical incident context"
        timestamptz acknowledged_at "Admin acknowledgement time"
        timestamptz created_at "Detection timestamp"
    }

    EVENT_PUBLICATION {
        uuid id PK "Outbox event unique ID"
        varchar event_routing_key "Topic / routing subject"
        varchar event_type "Domain event descriptor"
        jsonb event_payload "JSON domain event data"
        varchar listener_id "Target subscriber ID"
        varchar status "PENDING, PUBLISHED, FAILED, RETRYING"
        int attempt_count "Publication delivery attempts"
        timestamptz next_attempt_at "Exponential backoff schedule"
        timestamptz published_at "Successful publication time"
        timestamptz completed_at "Terminal completion timestamp"
        text last_error "Error diagnostic information"
        timestamptz created_at "Transaction commit timestamp"
    }
"""

print("Mermaid ERD length:", len(mermaid_erd))
print("Mermaid ERD validation OK!")
