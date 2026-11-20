package com.agentgate.invocation.guard;

import com.agentgate.killswitch.application.KillSwitchService;
import com.agentgate.shared.api.ErrorCode;
import com.agentgate.shared.exception.AgentGateException;
import com.agentgate.tool.domain.Tool;
import org.springframework.stereotype.Component;

@Component
public class KillSwitchGuard {

    private final KillSwitchService killSwitchService;

    public KillSwitchGuard(KillSwitchService killSwitchService) {
        this.killSwitchService = killSwitchService;
    }

    public void checkKillSwitch(String agentId, Tool tool) {
        if (killSwitchService.isExecutionBlocked(agentId, tool)) {
            throw new AgentGateException(ErrorCode.KILL_SWITCH_ACTIVE,
                    "Emergency Kill Switch is active for this operation. Invocation halted immediately.");
        }
    }
}
