package com.agentgate.killswitch.application;

import com.agentgate.killswitch.domain.KillSwitch;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public class KillSwitchDTOs {

    public record TripKillSwitchRequest(
        @NotNull(message = "Scope is required")
        KillSwitch.KillSwitchScope scope,

        String targetId,

        @NotBlank(message = "Reason is required")
        String reason,

        @NotBlank(message = "Admin ID is required")
        String adminId
    ) {}

    public record KillSwitchResponse(
        Long killSwitchId,
        String scope,
        String targetId,
        String status,
        String reason,
        String adminId,
        Instant activatedAt,
        Instant deactivatedAt
    ) {
        public static KillSwitchResponse from(KillSwitch ks) {
            return new KillSwitchResponse(
                ks.getKillSwitchId(),
                ks.getScope().name(),
                ks.getTargetId(),
                ks.getStatus().name(),
                ks.getReason(),
                ks.getAdminId(),
                ks.getActivatedAt(),
                ks.getDeactivatedAt()
            );
        }
    }
}
