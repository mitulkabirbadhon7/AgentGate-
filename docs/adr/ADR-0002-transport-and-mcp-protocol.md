# ADR-0002: Transport and Model Context Protocol (MCP) Binding

## Status
Accepted

## Context
AI agents interact with tools using the Model Context Protocol (MCP) over Streamable HTTP (JSON-RPC) as well as direct REST invocation endpoints. We need unified, safe ingress handling that complies with standard MCP semantics while enforcing strict governance.

## Decision
1. AgentGate exposes a unified `/mcp` HTTP endpoint conforming to MCP JSON-RPC protocol specification (`initialize`, `tools/list`, `tools/call`).
2. Tools hosted downstream may be either upstream MCP servers or OpenAPI-described REST endpoints. AgentGate adapts both into a unified discovery catalog presented to agents.
3. Unhealthy, deprecated, or blocked tools are omitted from `tools/list` responses to prevent agents from attempting invalid executions.

## Consequences
- AI agents see a single authoritative MCP server.
- Heterogeneous downstream tools (REST and MCP) are governed identically.
- Downstream outages are insulated from agents.
