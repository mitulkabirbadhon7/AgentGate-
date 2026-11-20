package com.agentgate.tool.api;

import com.agentgate.shared.api.ApiResponse;
import com.agentgate.tool.application.OpenApiImportService;
import com.agentgate.tool.application.ToolDTOs;
import com.agentgate.tool.application.ToolRegistryService;
import com.agentgate.tool.domain.Tool;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ToolAdminController {

    private final ToolRegistryService toolRegistryService;
    private final OpenApiImportService openApiImportService;

    public ToolAdminController(
            ToolRegistryService toolRegistryService,
            OpenApiImportService openApiImportService) {
        this.toolRegistryService = toolRegistryService;
        this.openApiImportService = openApiImportService;
    }

    // --- Downstream Servers ---

    @PostMapping("/servers")
    public ResponseEntity<ApiResponse<ToolDTOs.ServerResponse>> registerServer(
            @Valid @RequestBody ToolDTOs.CreateServerRequest request) {
        ToolDTOs.ServerResponse response = toolRegistryService.registerServer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Downstream server registered", response));
    }

    @GetMapping("/servers")
    public ResponseEntity<ApiResponse<List<ToolDTOs.ServerResponse>>> listServers() {
        return ResponseEntity.ok(ApiResponse.ok(toolRegistryService.listServers()));
    }

    @GetMapping("/servers/{serverId}")
    public ResponseEntity<ApiResponse<ToolDTOs.ServerResponse>> getServer(@PathVariable Long serverId) {
        return ResponseEntity.ok(ApiResponse.ok(toolRegistryService.getServer(serverId)));
    }

    @DeleteMapping("/servers/{serverId}")
    public ResponseEntity<ApiResponse<Void>> deleteServer(@PathVariable Long serverId) {
        toolRegistryService.deleteServer(serverId);
        return ResponseEntity.ok(ApiResponse.ok("Server deleted successfully", null));
    }

    // --- Tools ---

    @PostMapping("/tools")
    public ResponseEntity<ApiResponse<ToolDTOs.ToolResponse>> registerTool(
            @Valid @RequestBody ToolDTOs.CreateToolRequest request) {
        ToolDTOs.ToolResponse response = toolRegistryService.registerTool(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tool registered successfully", response));
    }

    @GetMapping("/tools")
    public ResponseEntity<ApiResponse<List<ToolDTOs.ToolResponse>>> listTools() {
        return ResponseEntity.ok(ApiResponse.ok(toolRegistryService.listTools()));
    }

    @GetMapping("/tools/{toolId}")
    public ResponseEntity<ApiResponse<ToolDTOs.ToolResponse>> getTool(@PathVariable Long toolId) {
        return ResponseEntity.ok(ApiResponse.ok(toolRegistryService.getTool(toolId)));
    }

    @PostMapping("/tools/{toolId}/status")
    public ResponseEntity<ApiResponse<String>> setToolStatus(
            @PathVariable Long toolId,
            @RequestParam Tool.ToolStatus status) {
        toolRegistryService.setToolStatus(toolId, status);
        return ResponseEntity.ok(ApiResponse.ok("Tool status updated to " + status, toolId.toString()));
    }

    @DeleteMapping("/tools/{toolId}")
    public ResponseEntity<ApiResponse<Void>> deleteTool(@PathVariable Long toolId) {
        toolRegistryService.deleteTool(toolId);
        return ResponseEntity.ok(ApiResponse.ok("Tool deleted successfully", null));
    }

    // --- OpenAPI Import ---

    @PostMapping("/tools/import-openapi")
    public ResponseEntity<ApiResponse<ToolDTOs.OpenApiImportSummary>> importOpenApi(
            @Valid @RequestBody ToolDTOs.OpenApiImportRequest request) {
        ToolDTOs.OpenApiImportSummary summary = openApiImportService.importOpenApi(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("OpenAPI imported successfully", summary));
    }
}
