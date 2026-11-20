package com.agentgate.killswitch.api;

import com.agentgate.killswitch.application.KillSwitchDTOs;
import com.agentgate.killswitch.application.KillSwitchService;
import com.agentgate.shared.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/killswitches")
public class KillSwitchAdminController {

    private final KillSwitchService killSwitchService;

    public KillSwitchAdminController(KillSwitchService killSwitchService) {
        this.killSwitchService = killSwitchService;
    }

    @PostMapping("/trip")
    public ResponseEntity<ApiResponse<KillSwitchDTOs.KillSwitchResponse>> trip(
            @Valid @RequestBody KillSwitchDTOs.TripKillSwitchRequest request) {
        KillSwitchDTOs.KillSwitchResponse response = killSwitchService.tripKillSwitch(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Kill switch activated", response));
    }

    @PostMapping("/{killSwitchId}/reset")
    public ResponseEntity<ApiResponse<KillSwitchDTOs.KillSwitchResponse>> reset(
            @PathVariable Long killSwitchId,
            @RequestParam String adminId) {
        KillSwitchDTOs.KillSwitchResponse response = killSwitchService.resetKillSwitch(killSwitchId, adminId);
        return ResponseEntity.ok(ApiResponse.ok("Kill switch deactivated", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<KillSwitchDTOs.KillSwitchResponse>>> listActive() {
        return ResponseEntity.ok(ApiResponse.ok(killSwitchService.listActiveSwitches()));
    }
}
