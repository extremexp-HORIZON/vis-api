package gr.imsi.athenarc.xtremexpvisapi.controller;

import gr.imsi.athenarc.xtremexpvisapi.domain.observability.TraceDetail;
import gr.imsi.athenarc.xtremexpvisapi.domain.observability.TracesResponse;
import gr.imsi.athenarc.xtremexpvisapi.service.observability.ObservabilityService;
import gr.imsi.athenarc.xtremexpvisapi.service.observability.ObservabilityServiceFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved traces"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  public ResponseEntity<TracesResponse> getTraces(
      @Parameter(description = "The project ID", required = true) @RequestParam String projectId,
      @Parameter(description = "The session ID") @RequestParam(required = false) String sessionId,
      @Parameter(description = "The user ID") @RequestParam(required = false) String userId) {
    TracesResponse traces = observabilityService.getTraces(projectId, sessionId, userId);
    return ResponseEntity.ok(traces);
  }

  @GetMapping("/traces/{traceId}")
  @Operation(
      summary = "Get a specific trace by ID",
      description =
          "Get a full trace from the observability service, including observations and scores.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the trace"),
        @ApiResponse(responseCode = "404", description = "Trace not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  public ResponseEntity<TraceDetail> getTrace(
      @Parameter(description = "The ID of the trace", required = true) @PathVariable
          String traceId) {
    TraceDetail trace = observabilityService.getTrace(traceId);
    return ResponseEntity.ok(trace);
  }
}
