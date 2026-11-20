"""
Generate clean, professional, publication-quality SVGs for AgentGate:
1. docs/diagrams/architecture_clean.svg
2. docs/diagrams/erd_clean.svg
"""

import os

def generate_architecture_svg():
    width = 1600
    height = 1420
    
    svg = []
    svg.append(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width} {height}" width="100%" height="100%" style="background-color: #0b0f19; font-family: -apple-system, BlinkMacSystemFont, \'Segoe UI\', Roboto, Helvetica, Arial, sans-serif;">')
    
    # Defs: gradients, filters, markers
    svg.append('''
    <defs>
        <linearGradient id="agentGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#4f46e5" />
            <stop offset="100%" stop-color="#7c3aed" />
        </linearGradient>
        <linearGradient id="securityGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#0284c7" />
            <stop offset="100%" stop-color="#0369a1" />
        </linearGradient>
        <linearGradient id="pipelineGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#b45309" />
            <stop offset="100%" stop-color="#d97706" />
        </linearGradient>
        <linearGradient id="guardGrad" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stop-color="#1e293b" />
            <stop offset="100%" stop-color="#334155" />
        </linearGradient>
        <linearGradient id="serviceGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#1e1b4b" />
            <stop offset="100%" stop-color="#312e81" />
        </linearGradient>
        <linearGradient id="infraGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#111827" />
            <stop offset="100%" stop-color="#1f2937" />
        </linearGradient>
        <linearGradient id="downstreamGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#065f46" />
            <stop offset="100%" stop-color="#047857" />
        </linearGradient>
        <linearGradient id="frontendGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#831843" />
            <stop offset="100%" stop-color="#be185d" />
        </linearGradient>

        <filter id="shadow" x="-5%" y="-5%" width="110%" height="110%">
            <feDropShadow dx="0" dy="4" stdDeviation="6" flood-color="#000000" flood-opacity="0.4" />
        </filter>
        <filter id="cardShadow" x="-5%" y="-5%" width="110%" height="110%">
            <feDropShadow dx="0" dy="2" stdDeviation="3" flood-color="#000000" flood-opacity="0.3" />
        </filter>

        <!-- Arrow markers -->
        <marker id="arrow-blue" viewBox="0 0 10 10" refX="6" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
            <path d="M 0 1 L 8 5 L 0 9 z" fill="#38bdf8" />
        </marker>
        <marker id="arrow-amber" viewBox="0 0 10 10" refX="6" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
            <path d="M 0 1 L 8 5 L 0 9 z" fill="#f59e0b" />
        </marker>
        <marker id="arrow-emerald" viewBox="0 0 10 10" refX="6" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
            <path d="M 0 1 L 8 5 L 0 9 z" fill="#10b981" />
        </marker>
        <marker id="arrow-purple" viewBox="0 0 10 10" refX="6" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
            <path d="M 0 1 L 8 5 L 0 9 z" fill="#a855f7" />
        </marker>
        <marker id="arrow-gray" viewBox="0 0 10 10" refX="6" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
            <path d="M 0 1 L 8 5 L 0 9 z" fill="#94a3b8" />
        </marker>
    </defs>
    ''')

    # Background grid pattern
    svg.append('''
    <pattern id="grid" width="40" height="40" patternUnits="userSpaceOnUse">
        <path d="M 40 0 L 0 0 0 40" fill="none" stroke="#1e293b" stroke-width="0.7" stroke-opacity="0.4"/>
    </pattern>
    <rect width="100%" height="100%" fill="url(#grid)" />
    ''')

    # Main Title Header
    svg.append('''
    <g transform="translate(50, 40)">
        <text x="0" y="0" font-size="28" font-weight="700" fill="#f8fafc" letter-spacing="-0.5">AgentGate System Architecture</text>
        <text x="0" y="24" font-size="14" font-weight="400" fill="#94a3b8">Spring Boot 4.x Modular Monolith &amp; AI-Agent Tool Governance Gateway (Single-Tenant)</text>
    </g>
    ''')

    # Top: AI Agents (External)
    svg.append('''
    <g transform="translate(850, 30)" filter="url(#shadow)">
        <rect x="0" y="0" width="680" height="90" rx="12" fill="#18181b" stroke="#3f3f46" stroke-width="1.5" />
        <rect x="0" y="0" width="680" height="28" rx="12" fill="url(#agentGrad)" />
        <rect x="0" y="16" width="680" height="12" fill="url(#agentGrad)" />
        <text x="20" y="19" font-size="13" font-weight="700" fill="#ffffff" letter-spacing="0.5">🤖 EXTERNAL AI AGENTS</text>
        
        <!-- Three Agent cards -->
        <g transform="translate(20, 38)">
            <rect x="0" y="0" width="190" height="40" rx="6" fill="#27272a" stroke="#52525b" stroke-width="1" />
            <text x="95" y="25" font-size="13" font-weight="600" fill="#e4e4e7" text-anchor="middle">Red-Team Client</text>
        </g>
        <g transform="translate(235, 38)">
            <rect x="0" y="0" width="190" height="40" rx="6" fill="#27272a" stroke="#52525b" stroke-width="1" />
            <text x="95" y="25" font-size="13" font-weight="600" fill="#e4e4e7" text-anchor="middle">Sales Agent</text>
        </g>
        <g transform="translate(450, 38)">
            <rect x="0" y="0" width="190" height="40" rx="6" fill="#27272a" stroke="#52525b" stroke-width="1" />
            <text x="95" y="25" font-size="13" font-weight="600" fill="#e4e4e7" text-anchor="middle">Support Bot</text>
        </g>
    </g>
    ''')

    # Big Container: AgentGate Core Boundary
    svg.append('''
    <g transform="translate(40, 160)" filter="url(#shadow)">
        <!-- Outer Gateway Box -->
        <rect x="0" y="0" width="1520" height="1060" rx="16" fill="#0f172a" stroke="#334155" stroke-width="2" />
        <rect x="0" y="0" width="1520" height="36" rx="16" fill="#1e293b" />
        <rect x="0" y="20" width="1520" height="16" fill="#1e293b" />
        <circle cx="24" cy="18" r="6" fill="#ef4444" />
        <circle cx="44" cy="18" r="6" fill="#f59e0b" />
        <circle cx="64" cy="18" r="6" fill="#10b981" />
        <text x="86" y="23" font-size="14" font-weight="700" fill="#38bdf8" letter-spacing="0.5">AGENTGATE CORE APPLICATION BOUNDARY — SPRING BOOT 4.X MODULAR MONOLITH</text>
    ''')

    # Inner Section 1: Security Layer (Top Right of inner)
    svg.append('''
        <g transform="translate(810, 60)">
            <rect x="0" y="0" width="670" height="180" rx="10" fill="#1e293b" stroke="#0ea5e9" stroke-width="1.5" />
            <rect x="0" y="0" width="670" height="28" rx="10" fill="url(#securityGrad)" />
            <rect x="0" y="16" width="670" height="12" fill="url(#securityGrad)" />
            <text x="18" y="19" font-size="13" font-weight="700" fill="#ffffff">🛡️ SECURITY LAYER &amp; INGRESS GATEWAY</text>

            <!-- OAuth2 Resource Server -->
            <g transform="translate(140, 38)">
                <rect x="0" y="0" width="390" height="34" rx="6" fill="#0369a1" stroke="#38bdf8" stroke-width="1" />
                <text x="195" y="22" font-size="13" font-weight="600" fill="#ffffff" text-anchor="middle">OAuth2 Resource Server (JWT / Introspection)</text>
            </g>

            <!-- Principal Injector -->
            <g transform="translate(140, 84)">
                <rect x="0" y="0" width="390" height="34" rx="6" fill="#0f172a" stroke="#38bdf8" stroke-width="1" />
                <text x="195" y="22" font-size="12" font-weight="600" fill="#bae6fd" text-anchor="middle">Principal Injector (_meta ➔ ContextSnapshot)</text>
            </g>

            <!-- 2 Entrypoints -->
            <g transform="translate(30, 130)">
                <rect x="0" y="0" width="280" height="36" rx="6" fill="#1e3a8a" stroke="#60a5fa" stroke-width="1.5" />
                <text x="140" y="23" font-size="13" font-weight="700" fill="#93c5fd" text-anchor="middle">MCP Endpoint (/mcp)</text>
            </g>
            <g transform="translate(360, 130)">
                <rect x="0" y="0" width="280" height="36" rx="6" fill="#1e3a8a" stroke="#60a5fa" stroke-width="1.5" />
                <text x="140" y="23" font-size="13" font-weight="700" fill="#93c5fd" text-anchor="middle">Admin REST API (/api/v1/*)</text>
            </g>
        </g>
    ''')

    # Inner Section 2: Domain Services & Invocation Lifecycle
    svg.append('''
        <g transform="translate(580, 260)">
            <!-- Invocation Service Main Card -->
            <rect x="0" y="0" width="450" height="740" rx="12" fill="#111827" stroke="#4b5563" stroke-width="1.5" />
            <rect x="0" y="0" width="450" height="28" rx="12" fill="#374151" />
            <rect x="0" y="16" width="450" height="12" fill="#374151" />
            <text x="16" y="19" font-size="13" font-weight="700" fill="#f3f4f6">⚙️ DOMAIN SERVICES &amp; ORCHESTRATION</text>

            <!-- Invocation Service -->
            <g transform="translate(30, 45)">
                <rect x="0" y="0" width="390" height="65" rx="8" fill="#1e1b4b" stroke="#6366f1" stroke-width="1.5" filter="url(#cardShadow)" />
                <text x="195" y="28" font-size="15" font-weight="700" fill="#e0e7ff" text-anchor="middle">Invocation Service</text>
                <text x="195" y="48" font-size="12" font-weight="400" fill="#a5b4fc" text-anchor="middle">Orchestrates Invocation Lifecycle &amp; State Transitions</text>
            </g>

            <!-- Audit Service -->
            <g transform="translate(30, 130)">
                <rect x="0" y="0" width="390" height="65" rx="8" fill="#1e293b" stroke="#3b82f6" stroke-width="1.5" filter="url(#cardShadow)" />
                <text x="195" y="28" font-size="15" font-weight="700" fill="#93c5fd" text-anchor="middle">Audit Service</text>
                <text x="195" y="48" font-size="12" font-weight="400" fill="#60a5fa" text-anchor="middle">Tamper-Evident Hash Chain (Advisory Lock Append)</text>
            </g>

            <!-- Operational Services grid inside Domain -->
            <g transform="translate(30, 215)">
                <!-- Alert Service -->
                <rect x="0" y="0" width="390" height="50" rx="6" fill="#1f2937" stroke="#ef4444" stroke-width="1" />
                <text x="20" y="30" font-size="13" font-weight="700" fill="#fca5a5">Alert Service</text>
                <text x="370" y="30" font-size="11" font-weight="500" fill="#9ca3af" text-anchor="end">Breach / Health / Security</text>
            </g>
            <g transform="translate(30, 280)">
                <!-- Kill Switch Service -->
                <rect x="0" y="0" width="390" height="50" rx="6" fill="#1f2937" stroke="#f97316" stroke-width="1" />
                <text x="20" y="30" font-size="13" font-weight="700" fill="#fdba74">Kill Switch Service</text>
                <text x="370" y="30" font-size="11" font-weight="500" fill="#9ca3af" text-anchor="end">Redis Mirrored &lt;10ms Sync</text>
            </g>
            <g transform="translate(30, 345)">
                <!-- Policy Service -->
                <rect x="0" y="0" width="390" height="50" rx="6" fill="#1f2937" stroke="#10b981" stroke-width="1" />
                <text x="20" y="30" font-size="13" font-weight="700" fill="#6ee7b7">Policy Service</text>
                <text x="370" y="30" font-size="11" font-weight="500" fill="#9ca3af" text-anchor="end">Binding Resolution &amp; Playground</text>
            </g>
            <g transform="translate(30, 410)">
                <!-- Tool Registry -->
                <rect x="0" y="0" width="390" height="50" rx="6" fill="#1f2937" stroke="#06b6d4" stroke-width="1" />
                <text x="20" y="30" font-size="13" font-weight="700" fill="#67e8f9">Tool Registry Service</text>
                <text x="370" y="30" font-size="11" font-weight="500" fill="#9ca3af" text-anchor="end">Per-Server Health &amp; SSRF Defense</text>
            </g>

            <!-- Health Manager Card (Bottom of domain services) -->
            <g transform="translate(30, 485)">
                <rect x="0" y="0" width="390" height="75" rx="8" fill="#042f2e" stroke="#14b8a6" stroke-width="1.5" filter="url(#cardShadow)" />
                <text x="195" y="30" font-size="15" font-weight="700" fill="#5eead4" text-anchor="middle">Health Manager</text>
                <text x="195" y="52" font-size="12" font-weight="400" fill="#99f6e4" text-anchor="middle">Redis Shared Circuit State (Resilience4j)</text>
            </g>
        </g>
    ''')

    # Inner Section 3: Governance Pipeline (Right Side)
    svg.append('''
        <g transform="translate(1060, 260)">
            <rect x="0" y="0" width="420" height="740" rx="12" fill="#1c1917" stroke="#d97706" stroke-width="1.5" />
            <rect x="0" y="0" width="420" height="28" rx="12" fill="url(#pipelineGrad)" />
            <rect x="0" y="16" width="420" height="12" fill="url(#pipelineGrad)" />
            <text x="16" y="19" font-size="13" font-weight="700" fill="#ffffff">🔒 GOVERNANCE PIPELINE (PLUGGABLE GUARDS)</text>

            <!-- Guard 1 -->
            <g transform="translate(20, 45)">
                <rect x="0" y="0" width="380" height="70" rx="8" fill="#292524" stroke="#78716c" stroke-width="1" />
                <rect x="12" y="12" width="28" height="28" rx="14" fill="#d97706" />
                <text x="26" y="31" font-size="13" font-weight="700" fill="#ffffff" text-anchor="middle">1</text>
                <text x="50" y="30" font-size="14" font-weight="700" fill="#fef3c7">SchemaValidationGuard</text>
                <text x="50" y="52" font-size="11" font-weight="400" fill="#a8a29e">Strict JSON Schema validation &amp; normalization</text>
            </g>

            <!-- Arrow 1 to 2 -->
            <path d="M 210 115 L 210 130" stroke="#f59e0b" stroke-width="2" marker-end="url(#arrow-amber)" />

            <!-- Guard 2 -->
            <g transform="translate(20, 135)">
                <rect x="0" y="0" width="380" height="70" rx="8" fill="#292524" stroke="#78716c" stroke-width="1" />
                <rect x="12" y="12" width="28" height="28" rx="14" fill="#d97706" />
                <text x="26" y="31" font-size="13" font-weight="700" fill="#ffffff" text-anchor="middle">2</text>
                <text x="50" y="30" font-size="14" font-weight="700" fill="#fef3c7">KillSwitchGuard</text>
                <text x="50" y="52" font-size="11" font-weight="400" fill="#a8a29e">Scopes: AGENT / TOOL / ALL_WRITE</text>
            </g>

            <!-- Arrow 2 to 3 -->
            <path d="M 210 205 L 210 220" stroke="#f59e0b" stroke-width="2" marker-end="url(#arrow-amber)" />

            <!-- Guard 3 -->
            <g transform="translate(20, 225)">
                <rect x="0" y="0" width="380" height="70" rx="8" fill="#292524" stroke="#78716c" stroke-width="1" />
                <rect x="12" y="12" width="28" height="28" rx="14" fill="#d97706" />
                <text x="26" y="31" font-size="13" font-weight="700" fill="#ffffff" text-anchor="middle">3</text>
                <text x="50" y="30" font-size="14" font-weight="700" fill="#fef3c7">PolicyEvaluator</text>
                <text x="50" y="52" font-size="11" font-weight="400" fill="#a8a29e">Immutable version-pinned rules, default-deny</text>
            </g>

            <!-- Arrow 3 to 4 -->
            <path d="M 210 295 L 210 310" stroke="#f59e0b" stroke-width="2" marker-end="url(#arrow-amber)" />

            <!-- Guard 4 -->
            <g transform="translate(20, 315)">
                <rect x="0" y="0" width="380" height="70" rx="8" fill="#292524" stroke="#78716c" stroke-width="1" />
                <rect x="12" y="12" width="28" height="28" rx="14" fill="#d97706" />
                <text x="26" y="31" font-size="13" font-weight="700" fill="#ffffff" text-anchor="middle">4</text>
                <text x="50" y="30" font-size="14" font-weight="700" fill="#fef3c7">RateLimit + Quota Guard</text>
                <text x="50" y="52" font-size="11" font-weight="400" fill="#a8a29e">Redis Lua token-bucket &amp; daily UTC spend caps</text>
            </g>

            <!-- Arrow 4 to 5 -->
            <path d="M 210 385 L 210 400" stroke="#f59e0b" stroke-width="2" marker-end="url(#arrow-amber)" />

            <!-- Guard 5 -->
            <g transform="translate(20, 405)">
                <rect x="0" y="0" width="380" height="70" rx="8" fill="#292524" stroke="#78716c" stroke-width="1" />
                <rect x="12" y="12" width="28" height="28" rx="14" fill="#d97706" />
                <text x="26" y="31" font-size="13" font-weight="700" fill="#ffffff" text-anchor="middle">5</text>
                <text x="50" y="30" font-size="14" font-weight="700" fill="#fef3c7">ApprovalGuard</text>
                <text x="50" y="52" font-size="11" font-weight="400" fill="#a8a29e">JPA State Machine (Pending/Approve/Reject)</text>
            </g>

            <!-- Arrow 5 to 6 -->
            <path d="M 210 475 L 210 490" stroke="#f59e0b" stroke-width="2" marker-end="url(#arrow-amber)" />

            <!-- Guard 6 -->
            <g transform="translate(20, 495)">
                <rect x="0" y="0" width="380" height="70" rx="8" fill="#292524" stroke="#78716c" stroke-width="1" />
                <rect x="12" y="12" width="28" height="28" rx="14" fill="#d97706" />
                <text x="26" y="31" font-size="13" font-weight="700" fill="#ffffff" text-anchor="middle">6</text>
                <text x="50" y="30" font-size="14" font-weight="700" fill="#fef3c7">RedactionGuard</text>
                <text x="50" y="52" font-size="11" font-weight="400" fill="#a8a29e">Irreversible mask + salted hash (PII &amp; NID)</text>
            </g>

            <!-- Exit condition -->
            <g transform="translate(20, 580)">
                <rect x="0" y="0" width="380" height="40" rx="6" fill="#064e3b" stroke="#10b981" stroke-width="1.5" />
                <text x="190" y="25" font-size="13" font-weight="700" fill="#a7f3d0" text-anchor="middle">PASS: Approved / Low Risk</text>
            </g>
        </g>
    ''')

    # Inner Section 4: Frontend (Thymeleaf UI) on Left Top
    svg.append('''
        <g transform="translate(40, 60)">
            <rect x="0" y="0" width="490" height="180" rx="10" fill="#1e1e24" stroke="#e11d48" stroke-width="1.5" />
            <rect x="0" y="0" width="490" height="28" rx="10" fill="url(#frontendGrad)" />
            <rect x="0" y="16" width="490" height="12" fill="url(#frontendGrad)" />
            <text x="18" y="19" font-size="13" font-weight="700" fill="#ffffff">🖥️ FRONTEND (THYMELEAF ADMIN UI)</text>

            <g transform="translate(25, 45)">
                <rect x="0" y="0" width="440" height="110" rx="8" fill="#27272a" stroke="#52525b" stroke-width="1" />
                <text x="220" y="30" font-size="15" font-weight="700" fill="#fda4af" text-anchor="middle">Admin Web Dashboard</text>
                <text x="220" y="55" font-size="12" font-weight="400" fill="#e4e4e7" text-anchor="middle">• Human Approvals &amp; Argument Modification</text>
                <text x="220" y="75" font-size="12" font-weight="400" fill="#e4e4e7" text-anchor="middle">• Policy Management &amp; Dry-Run Playground</text>
                <text x="220" y="95" font-size="12" font-weight="400" fill="#e4e4e7" text-anchor="middle">• Audit Verification • Kill Switches • Alert Triage</text>
            </g>
        </g>
    ''')

    # Inner Section 5: Infrastructure (PostgreSQL, Redis, Outbox) on Left Bottom
    svg.append('''
        <g transform="translate(40, 260)">
            <rect x="0" y="0" width="490" height="740" rx="12" fill="#0f172a" stroke="#475569" stroke-width="1.5" />
            <rect x="0" y="0" width="490" height="28" rx="12" fill="#334155" />
            <rect x="0" y="16" width="490" height="12" fill="#334155" />
            <text x="18" y="19" font-size="13" font-weight="700" fill="#f8fafc">💾 INFRASTRUCTURE &amp; DATA STORES</text>

            <!-- Outbox Dispatcher -->
            <g transform="translate(25, 45)">
                <rect x="0" y="0" width="440" height="85" rx="8" fill="#1e293b" stroke="#f97316" stroke-width="1.5" filter="url(#cardShadow)" />
                <text x="20" y="30" font-size="15" font-weight="700" fill="#fdba74">Outbox Dispatcher</text>
                <text x="20" y="52" font-size="12" font-weight="400" fill="#cbd5e1">Transactional Outbox Pattern &amp; Reliable Delivery</text>
                <text x="20" y="70" font-size="11" font-weight="500" fill="#94a3b8">attempt_count | exponential backoff | replay queue</text>
            </g>

            <!-- Redis -->
            <g transform="translate(25, 150)">
                <rect x="0" y="0" width="440" height="220" rx="8" fill="#1e1b4b" stroke="#a855f7" stroke-width="1.5" filter="url(#cardShadow)" />
                <rect x="15" y="15" width="30" height="30" rx="6" fill="#9333ea" />
                <text x="30" y="36" font-size="16" font-weight="700" fill="#ffffff" text-anchor="middle">R</text>
                <text x="55" y="36" font-size="16" font-weight="700" fill="#d8b4fe">Redis (In-Memory Fast Tier)</text>
                
                <text x="20" y="70" font-size="12" font-weight="600" fill="#c084fc">• Rate-limit counters (Atomic Lua Scripts + TIME)</text>
                <text x="20" y="95" font-size="12" font-weight="600" fill="#c084fc">• Policy Cache &amp; Pub/Sub Invalidation (&lt;10s propagation)</text>
                <text x="20" y="120" font-size="12" font-weight="600" fill="#c084fc">• Shared Circuit Breaker State &amp; Kill-Switch Mirror</text>
                <text x="20" y="145" font-size="12" font-weight="600" fill="#c084fc">• Active Tools &amp; Discovery List Cache</text>
                
                <g transform="translate(15, 165)">
                    <rect x="0" y="0" width="410" height="40" rx="6" fill="#3b0764" stroke="#7e22ce" stroke-width="1" />
                    <text x="205" y="25" font-size="11" font-weight="600" fill="#e9d5ff" text-anchor="middle">Pub/Sub: alert | killswitch | policy | circuit</text>
                </g>
            </g>

            <!-- PostgreSQL -->
            <g transform="translate(25, 395)">
                <rect x="0" y="0" width="440" height="295" rx="8" fill="#0f2b38" stroke="#0ea5e9" stroke-width="1.5" filter="url(#cardShadow)" />
                <rect x="15" y="15" width="30" height="30" rx="6" fill="#0284c7" />
                <text x="30" y="36" font-size="16" font-weight="700" fill="#ffffff" text-anchor="middle">P</text>
                <text x="55" y="36" font-size="16" font-weight="700" fill="#7dd3fc">PostgreSQL (Durable Relational Store)</text>
                
                <text x="20" y="70" font-size="12" font-weight="600" fill="#bae6fd">• TOOL_INVOCATION &amp; AUDIT_EVENT (Advisory Lock Chained)</text>
                <text x="20" y="95" font-size="12" font-weight="600" fill="#bae6fd">• APPROVAL_REQUEST (JPA State Machine + Optimistic Lock)</text>
                <text x="20" y="120" font-size="12" font-weight="600" fill="#bae6fd">• POLICY &amp; Immutable POLICY_VERSION &amp; POLICY_BINDING</text>
                <text x="20" y="145" font-size="12" font-weight="600" fill="#bae6fd">• KILL_SWITCH, QUOTA_POLICY, RATE_LIMIT_POLICY</text>
                <text x="20" y="170" font-size="12" font-weight="600" fill="#bae6fd">• EVENT_PUBLICATION (Transactional Outbox Rows)</text>
                <text x="20" y="195" font-size="12" font-weight="600" fill="#bae6fd">• AGENT_CLIENT &amp; END_USER Identity Claims</text>
                <text x="20" y="220" font-size="12" font-weight="600" fill="#bae6fd">• SECURITY_ALERT Logging &amp; Triage</text>

                <g transform="translate(15, 245)">
                    <rect x="0" y="0" width="410" height="35" rx="6" fill="#082f49" stroke="#0369a1" stroke-width="1" />
                    <text x="205" y="22" font-size="11" font-weight="600" fill="#38bdf8" text-anchor="middle">Flyway Managed Baseline Migrations &amp; DB Constraints</text>
                </g>
            </g>
        </g>
    ''')

    # Close AgentGate Core Box
    svg.append('</g>')

    # Bottom: Downstream Targets Layer
    svg.append('''
    <g transform="translate(480, 1260)" filter="url(#shadow)">
        <rect x="0" y="0" width="1080" height="110" rx="12" fill="#064e3b" stroke="#10b981" stroke-width="1.5" />
        <rect x="0" y="0" width="1080" height="28" rx="12" fill="url(#downstreamGrad)" />
        <rect x="0" y="16" width="1080" height="12" fill="url(#downstreamGrad)" />
        <text x="20" y="19" font-size="13" font-weight="700" fill="#ffffff" letter-spacing="0.5">🔌 DOWNSTREAM TARGET SERVERS (INTERNAL &amp; EXTERNAL)</text>

        <!-- Downstream targets -->
        <g transform="translate(40, 45)">
            <rect x="0" y="0" width="300" height="50" rx="8" fill="#022c22" stroke="#34d399" stroke-width="1" />
            <text x="150" y="26" font-size="13" font-weight="700" fill="#a7f3d0" text-anchor="middle">MCP Server A (Refund)</text>
            <text x="150" y="42" font-size="10" font-weight="400" fill="#6ee7b7" text-anchor="middle">JSON-RPC over Streamable HTTP</text>
        </g>
        <g transform="translate(390, 45)">
            <rect x="0" y="0" width="300" height="50" rx="8" fill="#022c22" stroke="#34d399" stroke-width="1" />
            <text x="150" y="26" font-size="13" font-weight="700" fill="#a7f3d0" text-anchor="middle">MCP Server B (Order)</text>
            <text x="150" y="42" font-size="10" font-weight="400" fill="#6ee7b7" text-anchor="middle">Order Management &amp; Fulfillment</text>
        </g>
        <g transform="translate(740, 45)">
            <rect x="0" y="0" width="300" height="50" rx="8" fill="#022c22" stroke="#34d399" stroke-width="1" />
            <text x="150" y="26" font-size="13" font-weight="700" fill="#a7f3d0" text-anchor="middle">REST API (Customer)</text>
            <text x="150" y="42" font-size="10" font-weight="400" fill="#6ee7b7" text-anchor="middle">OpenAPI Imported / Bounded Timeout</text>
        </g>
    </g>
    ''')

    # Connecting Flow Lines & Labels
    svg.append('''
    <!-- Agents to Security Layer -->
    <path d="M 1190 120 L 1190 220" stroke="#38bdf8" stroke-width="3" stroke-dasharray="6,4" marker-end="url(#arrow-blue)" />
    <rect x="1115" y="145" width="150" height="22" rx="4" fill="#0f172a" stroke="#0284c7" stroke-width="1" />
    <text x="1190" y="160" font-size="11" font-weight="600" fill="#7dd3fc" text-anchor="middle">Bearer Token (OAuth2)</text>

    <!-- MCP Ingress to Invocation Service -->
    <path d="M 990 390 L 990 420 L 830 420 L 830 465" stroke="#38bdf8" stroke-width="2.5" marker-end="url(#arrow-blue)" fill="none" />
    <rect x="850" y="405" width="130" height="20" rx="4" fill="#0f172a" stroke="#0284c7" stroke-width="1" />
    <text x="915" y="419" font-size="10" font-weight="600" fill="#7dd3fc" text-anchor="middle">Tool Call Request</text>

    <!-- Invocation Service to Governance Pipeline -->
    <path d="M 1030 495 L 1080 495 L 1080 340 L 1100 340" stroke="#f59e0b" stroke-width="2.5" marker-end="url(#arrow-amber)" fill="none" />
    
    <!-- Governance Pipeline Exit to Health Manager -->
    <path d="M 1270 880 L 1270 940 L 1000 940 L 1000 780 L 970 780" stroke="#10b981" stroke-width="2.5" marker-end="url(#arrow-emerald)" fill="none" />

    <!-- Health Manager to Downstream Targets -->
    <path d="M 805 1000 L 805 1250" stroke="#10b981" stroke-width="3" marker-end="url(#arrow-emerald)" fill="none" />
    <rect x="730" y="1120" width="150" height="24" rx="4" fill="#022c22" stroke="#059669" stroke-width="1" />
    <text x="805" y="1136" font-size="11" font-weight="700" fill="#a7f3d0" text-anchor="middle">Authorized Execution</text>

    <!-- Audit Service to PostgreSQL -->
    <path d="M 610 560 L 530 560 L 530 680" stroke="#38bdf8" stroke-width="2" marker-end="url(#arrow-blue)" fill="none" />
    <rect x="470" y="605" width="130" height="20" rx="4" fill="#0f172a" stroke="#0284c7" stroke-width="1" />
    <text x="535" y="619" font-size="10" font-weight="600" fill="#38bdf8" text-anchor="middle">Advisory Lock Append</text>

    <!-- Redis to Governance/Services -->
    <path d="M 530 450 L 610 450" stroke="#a855f7" stroke-width="2" marker-end="url(#arrow-purple)" fill="none" />
    <rect x="525" y="435" width="110" height="20" rx="4" fill="#1e1b4b" stroke="#7e22ce" stroke-width="1" />
    <text x="580" y="449" font-size="10" font-weight="600" fill="#d8b4fe" text-anchor="middle">Pub/Sub &amp; Mirror</text>
    ''')

    svg.append('</svg>')
    return '\n'.join(svg)


def generate_erd_svg():
    width = 2400
    height = 1680

    svg = []
    svg.append(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width} {height}" width="100%" height="100%" style="background-color: #0b0f19; font-family: -apple-system, BlinkMacSystemFont, \'Segoe UI\', Roboto, Helvetica, Arial, sans-serif;">')

    # Defs
    svg.append('''
    <defs>
        <filter id="erdCardShadow" x="-5%" y="-5%" width="110%" height="110%">
            <feDropShadow dx="0" dy="4" stdDeviation="5" flood-color="#000000" flood-opacity="0.4" />
        </filter>
        <linearGradient id="headerGradIndigo" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stop-color="#3730a3" />
            <stop offset="100%" stop-color="#4f46e5" />
        </linearGradient>
        <linearGradient id="headerGradCyan" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stop-color="#0e7490" />
            <stop offset="100%" stop-color="#06b6d4" />
        </linearGradient>
        <linearGradient id="headerGradAmber" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stop-color="#b45309" />
            <stop offset="100%" stop-color="#d97706" />
        </linearGradient>
        <linearGradient id="headerGradEmerald" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stop-color="#047857" />
            <stop offset="100%" stop-color="#10b981" />
        </linearGradient>
        <linearGradient id="headerGradRose" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stop-color="#be123c" />
            <stop offset="100%" stop-color="#f43f5e" />
        </linearGradient>
        <linearGradient id="headerGradBlue" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stop-color="#1d4ed8" />
            <stop offset="100%" stop-color="#3b82f6" />
        </linearGradient>

        <marker id="crow-foot" viewBox="0 0 12 12" refX="10" refY="6" markerWidth="8" markerHeight="8" orient="auto-start-reverse">
            <path d="M 0 0 L 10 6 L 0 12 M 10 0 L 10 12" fill="none" stroke="#64748b" stroke-width="1.5" />
        </marker>
        <marker id="one-one" viewBox="0 0 12 12" refX="10" refY="6" markerWidth="8" markerHeight="8" orient="auto-start-reverse">
            <path d="M 6 0 L 6 12 M 10 0 L 10 12" fill="none" stroke="#64748b" stroke-width="1.5" />
        </marker>
    </defs>
    ''')

    # Grid Pattern
    svg.append('''
    <pattern id="gridErd" width="40" height="40" patternUnits="userSpaceOnUse">
        <path d="M 40 0 L 0 0 0 40" fill="none" stroke="#1e293b" stroke-width="0.7" stroke-opacity="0.4"/>
    </pattern>
    <rect width="100%" height="100%" fill="url(#gridErd)" />
    ''')

    # Header Title
    svg.append('''
    <g transform="translate(60, 45)">
        <text x="0" y="0" font-size="32" font-weight="700" fill="#f8fafc" letter-spacing="-0.5">AgentGate Data Model &amp; Entity-Relationship Diagram (ERD)</text>
        <text x="0" y="28" font-size="15" font-weight="400" fill="#94a3b8">Comprehensive PostgreSQL Schema: 15 Entities, Relational Invariants, Cryptographic Audit Chain &amp; Outbox</text>
    </g>
    ''')

    # Helper function to render a table card
    def render_table(x, y, width, title, grad, cols):
        h = 42 + len(cols) * 26 + 10
        out = [f'<g transform="translate({x}, {y})" filter="url(#erdCardShadow)">']
        out.append(f'<rect x="0" y="0" width="{width}" height="{h}" rx="10" fill="#1e293b" stroke="#334155" stroke-width="1.5" />')
        out.append(f'<rect x="0" y="0" width="{width}" height="38" rx="10" fill="url(#{grad})" />')
        out.append(f'<rect x="0" y="24" width="{width}" height="14" fill="url(#{grad})" />')
        out.append(f'<text x="16" y="25" font-size="14" font-weight="700" fill="#ffffff" letter-spacing="0.5">{title}</text>')
        
        # columns
        cy = 58
        for name, dtype, badge in cols:
            # alternating row background
            # badge color
            out.append(f'<text x="16" y="{cy}" font-size="12" font-weight="600" fill="#f1f5f9">{name}</text>')
            out.append(f'<text x="{width - 70}" y="{cy}" font-size="11" font-weight="500" fill="#94a3b8" text-anchor="end">{dtype}</text>')
            if badge == 'PK':
                out.append(f'<rect x="{width - 60}" y="{cy - 12}" width="45" height="18" rx="4" fill="#b91c1c" />')
                out.append(f'<text x="{width - 37}" y="{cy + 1}" font-size="10" font-weight="700" fill="#fee2e2" text-anchor="middle">PK</text>')
            elif badge == 'FK':
                out.append(f'<rect x="{width - 60}" y="{cy - 12}" width="45" height="18" rx="4" fill="#0369a1" />')
                out.append(f'<text x="{width - 37}" y="{cy + 1}" font-size="10" font-weight="700" fill="#e0f2fe" text-anchor="middle">FK</text>')
            elif badge == 'UQ':
                out.append(f'<rect x="{width - 60}" y="{cy - 12}" width="45" height="18" rx="4" fill="#047857" />')
                out.append(f'<text x="{width - 37}" y="{cy + 1}" font-size="10" font-weight="700" fill="#d1fae5" text-anchor="middle">UQ</text>')
            cy += 26
            
        out.append('</g>')
        return '\n'.join(out)

    # Table 1: AGENT_CLIENT
    svg.append(render_table(60, 110, 320, 'AGENT_CLIENT', 'headerGradIndigo', [
        ('client_id', 'VARCHAR(64)', 'PK'),
        ('name', 'VARCHAR(128)', ''),
        ('client_secret_hash', 'VARCHAR(255)', ''),
        ('display_name', 'VARCHAR(128)', ''),
        ('status', 'VARCHAR(32)', ''),
        ('created_at', 'TIMESTAMPTZ', '')
    ]))

    # Table 2: END_USER
    svg.append(render_table(420, 110, 300, 'END_USER', 'headerGradIndigo', [
        ('user_id', 'VARCHAR(64)', 'PK'),
        ('external_ref', 'VARCHAR(128)', 'UQ'),
        ('clearance_level', 'VARCHAR(32)', ''),
        ('created_at', 'TIMESTAMPTZ', '')
    ]))

    # Table 3: DOWNSTREAM_SERVER
    svg.append(render_table(760, 110, 360, 'DOWNSTREAM_SERVER', 'headerGradCyan', [
        ('server_id', 'BIGINT', 'PK'),
        ('name', 'VARCHAR(128)', 'UQ'),
        ('type', 'VARCHAR(32)', ''),
        ('endpoint_url', 'VARCHAR(512)', ''),
        ('health_check_interval_sec', 'INT', ''),
        ('circuit_open_threshold', 'INT', ''),
        ('created_at', 'TIMESTAMPTZ', '')
    ]))

    # Table 4: TOOL
    svg.append(render_table(1160, 110, 360, 'TOOL', 'headerGradCyan', [
        ('tool_id', 'BIGINT', 'PK'),
        ('server_id', 'BIGINT', 'FK'),
        ('name', 'VARCHAR(128)', 'UQ'),
        ('description', 'TEXT', ''),
        ('input_schema', 'JSONB', ''),
        ('risk_level', 'VARCHAR(32)', ''),
        ('routing_hint', 'VARCHAR(128)', ''),
        ('status', 'VARCHAR(32)', ''),
        ('created_at', 'TIMESTAMPTZ', '')
    ]))

    # Table 5: POLICY
    svg.append(render_table(1560, 110, 360, 'POLICY', 'headerGradEmerald', [
        ('policy_id', 'BIGINT', 'PK'),
        ('name', 'VARCHAR(128)', 'UQ'),
        ('status', 'VARCHAR(32)', ''),
        ('current_version_id', 'BIGINT', 'FK'),
        ('active_version_locks', 'BOOLEAN', ''),
        ('created_at', 'TIMESTAMPTZ', '')
    ]))

    # Table 6: POLICY_VERSION
    svg.append(render_table(1960, 110, 380, 'POLICY_VERSION', 'headerGradEmerald', [
        ('version_id', 'BIGINT', 'PK'),
        ('policy_id', 'BIGINT', 'FK'),
        ('version_number', 'INT', ''),
        ('rules_tree', 'JSONB', ''),
        ('effective_from', 'TIMESTAMPTZ', ''),
        ('audit_by_user', 'VARCHAR(128)', ''),
        ('version_hash', 'VARCHAR(64)', ''),
        ('created_at', 'TIMESTAMPTZ', '')
    ]))

    # Table 7: POLICY_BINDING
    svg.append(render_table(1560, 420, 360, 'POLICY_BINDING', 'headerGradEmerald', [
        ('binding_id', 'BIGINT', 'PK'),
        ('policy_id', 'BIGINT', 'FK'),
        ('agent_id', 'VARCHAR(64)', 'FK'),
        ('tool_id', 'BIGINT', 'FK'),
        ('scope', 'VARCHAR(32)', ''),
        ('priority', 'INT', ''),
        ('created_at', 'TIMESTAMPTZ', ''),
        ('updated_at', 'TIMESTAMPTZ', '')
    ]))

    # Table 8: KILL_SWITCH
    svg.append(render_table(1960, 420, 380, 'KILL_SWITCH', 'headerGradAmber', [
        ('kill_switch_id', 'BIGINT', 'PK'),
        ('scope', 'VARCHAR(32)', ''),
        ('target_id', 'VARCHAR(128)', ''),
        ('status', 'VARCHAR(32)', ''),
        ('reason', 'TEXT', ''),
        ('admin_id', 'VARCHAR(128)', ''),
        ('activated_at', 'TIMESTAMPTZ', ''),
        ('deactivated_at', 'TIMESTAMPTZ', '')
    ]))

    # Table 9: RATE_LIMIT_POLICY
    svg.append(render_table(60, 450, 320, 'RATE_LIMIT_POLICY', 'headerGradAmber', [
        ('rate_limit_id', 'BIGINT', 'PK'),
        ('scope', 'VARCHAR(32)', ''),
        ('agent_id', 'VARCHAR(64)', 'FK'),
        ('tool_id', 'BIGINT', 'FK'),
        ('window_seconds', 'INT', ''),
        ('max_requests', 'INT', ''),
        ('created_at', 'TIMESTAMPTZ', '')
    ]))

    # Table 10: QUOTA_POLICY
    svg.append(render_table(420, 450, 300, 'QUOTA_POLICY', 'headerGradAmber', [
        ('quota_id', 'BIGINT', 'PK'),
        ('scope', 'VARCHAR(32)', ''),
        ('agent_id', 'VARCHAR(64)', 'FK'),
        ('tool_id', 'BIGINT', 'FK'),
        ('max_amount', 'BIGINT', ''),
        ('window_unit', 'VARCHAR(32)', ''),
        ('created_at', 'TIMESTAMPTZ', '')
    ]))

    # Table 11: TOOL_INVOCATION (Center Core Table)
    svg.append(render_table(760, 450, 420, 'TOOL_INVOCATION', 'headerGradBlue', [
        ('invocation_id', 'BIGINT', 'PK'),
        ('event_id', 'UUID', 'UQ'),
        ('agent_id', 'VARCHAR(64)', 'FK'),
        ('user_id', 'VARCHAR(64)', 'FK'),
        ('tool_id', 'BIGINT', 'FK'),
        ('request_arguments_redacted', 'JSONB', ''),
        ('response_payload_redacted', 'JSONB', ''),
        ('status', 'VARCHAR(32)', ''),
        ('error_code', 'VARCHAR(64)', ''),
        ('policy_version_id', 'BIGINT', 'FK'),
        ('latency_ms', 'INT', ''),
        ('created_at', 'TIMESTAMPTZ', ''),
        ('completed_at', 'TIMESTAMPTZ', '')
    ]))

    # Table 12: APPROVAL_REQUEST
    svg.append(render_table(1220, 450, 300, 'APPROVAL_REQUEST', 'headerGradAmber', [
        ('approval_id', 'BIGINT', 'PK'),
        ('invocation_id', 'BIGINT', 'FK'),
        ('status', 'VARCHAR(32)', ''),
        ('original_arguments_encrypted', 'JSONB', ''),
        ('edited_arguments_encrypted', 'JSONB', ''),
        ('approver_id', 'VARCHAR(128)', ''),
        ('decision_reason', 'TEXT', ''),
        ('arguments_hash', 'VARCHAR(64)', ''),
        ('version', 'INT', ''),
        ('expires_at', 'TIMESTAMPTZ', ''),
        ('created_at', 'TIMESTAMPTZ', ''),
        ('updated_at', 'TIMESTAMPTZ', '')
    ]))

    # Table 13: AUDIT_EVENT
    svg.append(render_table(760, 930, 420, 'AUDIT_EVENT', 'headerGradBlue', [
        ('audit_id', 'BIGINT', 'PK'),
        ('invocation_id', 'BIGINT', 'FK'),
        ('sequence_number', 'BIGINT', ''),
        ('event_type', 'VARCHAR(64)', ''),
        ('event_time', 'TIMESTAMPTZ', ''),
        ('payload', 'JSONB', ''),
        ('prev_hash', 'VARCHAR(64)', ''),
        ('curr_hash', 'VARCHAR(64)', ''),
        ('hash_algorithm', 'VARCHAR(32)', ''),
        ('actor', 'VARCHAR(128)', '')
    ]))

    # Table 14: SECURITY_ALERT
    svg.append(render_table(1220, 930, 340, 'SECURITY_ALERT', 'headerGradRose', [
        ('alert_id', 'BIGINT', 'PK'),
        ('alert_type', 'VARCHAR(64)', ''),
        ('severity', 'VARCHAR(32)', ''),
        ('triggered_by', 'VARCHAR(64)', 'FK'),
        ('event_id', 'VARCHAR(128)', ''),
        ('details', 'TEXT', ''),
        ('acknowledged_at', 'TIMESTAMPTZ', ''),
        ('created_at', 'TIMESTAMPTZ', '')
    ]))

    # Table 15: EVENT_PUBLICATION (OUTBOX_EVENT)
    svg.append(render_table(1600, 930, 380, 'EVENT_PUBLICATION (OUTBOX)', 'headerGradRose', [
        ('id', 'UUID', 'PK'),
        ('event_routing_key', 'VARCHAR(255)', ''),
        ('event_type', 'VARCHAR(128)', ''),
        ('event_payload', 'JSONB', ''),
        ('listener_id', 'VARCHAR(128)', ''),
        ('status', 'VARCHAR(32)', ''),
        ('attempt_count', 'INT', ''),
        ('next_attempt_at', 'TIMESTAMPTZ', ''),
        ('published_at', 'TIMESTAMPTZ', ''),
        ('completed_at', 'TIMESTAMPTZ', ''),
        ('last_error', 'TEXT', ''),
        ('created_at', 'TIMESTAMPTZ', '')
    ]))

    # Explanatory Notes Box (Bottom Left)
    svg.append('''
    <g transform="translate(60, 830)" filter="url(#erdCardShadow)">
        <rect x="0" y="0" width="660" height="480" rx="12" fill="#0f172a" stroke="#334155" stroke-width="1.5" />
        <rect x="0" y="0" width="660" height="34" rx="12" fill="#1e293b" />
        <rect x="0" y="20" width="660" height="14" fill="#1e293b" />
        <text x="20" y="23" font-size="14" font-weight="700" fill="#38bdf8">KEY ARCHITECTURAL &amp; RELATIONAL INVARIANTS</text>

        <text x="25" y="65" font-size="13" font-weight="700" fill="#f8fafc">1. Cryptographic Audit Hash Chain (Advisory Locked)</text>
        <text x="25" y="85" font-size="12" font-weight="400" fill="#94a3b8">AUDIT_EVENT rows form an immutable hash chain: curr_hash = SHA256(prev_hash + sequence + payload).</text>
        <text x="25" y="103" font-size="12" font-weight="400" fill="#94a3b8">Appends are serialized via PostgreSQL transaction-level advisory locks to eliminate race conditions.</text>

        <text x="25" y="140" font-size="13" font-weight="700" fill="#f8fafc">2. Version-Pinned Policy Evaluation</text>
        <text x="25" y="160" font-size="12" font-weight="400" fill="#94a3b8">Every TOOL_INVOCATION pins a specific, immutable POLICY_VERSION row. Once published,</text>
        <text x="25" y="178" font-size="12" font-weight="400" fill="#94a3b8">a version can never be mutated, ensuring 100% deterministic post-incident auditability.</text>

        <text x="25" y="215" font-size="13" font-weight="700" fill="#f8fafc">3. Human-in-the-Loop Approval &amp; Argument Digest</text>
        <text x="25" y="235" font-size="12" font-weight="400" fill="#94a3b8">APPROVAL_REQUEST holds a 1:1 relationship with TOOL_INVOCATION. Edited arguments are encrypted</text>
        <text x="25" y="253" font-size="12" font-weight="400" fill="#94a3b8">and bound to arguments_hash; modified calls must pass fresh schema and policy re-validation.</text>

        <text x="25" y="290" font-size="13" font-weight="700" fill="#f8fafc">4. Scoped Emergency Kill Switches</text>
        <text x="25" y="310" font-size="12" font-weight="400" fill="#94a3b8">KILL_SWITCH supports GLOBAL_WRITES, AGENT, and TOOL scopes, mirrored into Redis for sub-10ms</text>
        <text x="25" y="328" font-size="12" font-weight="400" fill="#94a3b8">interception and immediately canceling affected pending approval requests.</text>

        <text x="25" y="365" font-size="13" font-weight="700" fill="#f8fafc">5. Transactional Outbox Pattern</text>
        <text x="25" y="385" font-size="12" font-weight="400" fill="#94a3b8">EVENT_PUBLICATION records are written inside the exact same DB transaction as business mutations,</text>
        <text x="25" y="403" font-size="12" font-weight="400" fill="#94a3b8">guaranteeing at-least-once event delivery to external subscribers without dual-write hazards.</text>

        <g transform="translate(25, 425)">
            <rect x="0" y="0" width="610" height="35" rx="6" fill="#1e293b" stroke="#475569" stroke-width="1" />
            <text x="305" y="22" font-size="12" font-weight="600" fill="#cbd5e1" text-anchor="middle">Data Integrity: Flyway DDL • Strict FK Constraints • JSONB Schemas • Optimistic Locking</text>
        </g>
    </g>
    ''')

    # Relationship connector lines (Subtle clean lines)
    svg.append('''
    <!-- DOWNSTREAM_SERVER to TOOL -->
    <path d="M 1120 180 L 1160 180" stroke="#06b6d4" stroke-width="2" marker-start="url(#one-one)" marker-end="url(#crow-foot)" />

    <!-- AGENT_CLIENT to TOOL_INVOCATION -->
    <path d="M 220 280 L 220 400 L 760 520" stroke="#6366f1" stroke-width="1.8" stroke-dasharray="5,4" marker-end="url(#crow-foot)" fill="none" />

    <!-- TOOL to TOOL_INVOCATION -->
    <path d="M 1340 360 L 1340 400 L 1050 400 L 1050 450" stroke="#06b6d4" stroke-width="2" marker-start="url(#one-one)" marker-end="url(#crow-foot)" fill="none" />

    <!-- TOOL_INVOCATION to APPROVAL_REQUEST (1:1) -->
    <path d="M 1180 500 L 1220 500" stroke="#f59e0b" stroke-width="2" marker-start="url(#one-one)" marker-end="url(#one-one)" />

    <!-- TOOL_INVOCATION to AUDIT_EVENT (1:N) -->
    <path d="M 970 870 L 970 930" stroke="#3b82f6" stroke-width="2.5" marker-start="url(#one-one)" marker-end="url(#crow-foot)" />

    <!-- POLICY to POLICY_VERSION -->
    <path d="M 1920 180 L 1960 180" stroke="#10b981" stroke-width="2" marker-start="url(#one-one)" marker-end="url(#crow-foot)" />

    <!-- POLICY to POLICY_BINDING -->
    <path d="M 1740 280 L 1740 420" stroke="#10b981" stroke-width="2" marker-start="url(#one-one)" marker-end="url(#crow-foot)" />

    <!-- TOOL_INVOCATION to SECURITY_ALERT -->
    <path d="M 1180 720 L 1350 720 L 1350 930" stroke="#f43f5e" stroke-width="1.8" stroke-dasharray="5,4" marker-end="url(#crow-foot)" fill="none" />
    ''')

    svg.append('</svg>')
    return '\n'.join(svg)


def main():
    os.makedirs('docs/diagrams', exist_ok=True)
    
    arch_svg = generate_architecture_svg()
    with open('docs/diagrams/architecture_clean.svg', 'w', encoding='utf-8') as f:
        f.write(arch_svg)
    print('Generated docs/diagrams/architecture_clean.svg successfully.')
    
    erd_svg = generate_erd_svg()
    with open('docs/diagrams/erd_clean.svg', 'w', encoding='utf-8') as f:
        f.write(erd_svg)
    print('Generated docs/diagrams/erd_clean.svg successfully.')

if __name__ == '__main__':
    main()
