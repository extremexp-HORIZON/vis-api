package gr.imsi.athenarc.xtremexpvisapi.controller;

import gr.imsi.athenarc.xtremexpvisapi.domain.observability.TracesResponse;
import gr.imsi.athenarc.xtremexpvisapi.service.observability.ObservabilityService;
import gr.imsi.athenarc.xtremexpvisapi.service.observability.ObservabilityServiceFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/observability")
public class ObservabilityController {

    private final ObservabilityService observabilityService;

    public ObservabilityController(ObservabilityServiceFactory observabilityServiceFactory) {
        this.observabilityService = observabilityServiceFactory.getObservabilityService();
    }

    @GetMapping("/traces")
    @Operation(summary = "Get traces", description = "Get traces from the observability service.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved traces"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<TracesResponse> getTraces(
            @Parameter(description = "The project ID", required = true) @RequestParam String projectId,
            @Parameter(description = "The session ID") @RequestParam(required = false) String sessionId) {
        TracesResponse traces = observabilityService.getTraces(projectId, sessionId);
        return ResponseEntity.ok(traces);
    }
}
