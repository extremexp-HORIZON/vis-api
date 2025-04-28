package gr.imsi.athenarc.xtremexpvisapi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Experiment;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Run;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.UserEvaluation;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.UserEvaluationResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Metric;
import gr.imsi.athenarc.xtremexpvisapi.service.ExperimentServiceFactory;

import java.util.List;

/**
 * REST controller that provides experiment tracking endpoints.
 * This controller delegates requests to the appropriate execution engine
 * implementation
 * (e.g., MLflow, ExtremeXP).
 * It provides endpoints to retrieve experiments, fetch specific experiment
 * details,
 * list associated runs, retrieve specific run details, and fetch recorded
 * metric values.
 */
@RestController
@RequestMapping("/experiments")
public class ExperimentController {

    private final ExperimentServiceFactory experimentServiceFactory;

    @Autowired
    public ExperimentController(ExperimentServiceFactory experimentServiceFactory) {
        this.experimentServiceFactory = experimentServiceFactory;
    }

    /**
     * Retrieves a paginated list of experiments.
     * Supports pagination with `limit` and `offset` parameters.
     *
     * @param limit  The maximum number of experiments to return (default: 10, max:
     *               100).
     * @param offset The starting index for retrieval (default: 0).
     * @return A ResponseEntity containing a list of experiments and their total
     *         count.
     */
    @GetMapping
    public ResponseEntity<List<Experiment>> getExperiments(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return experimentServiceFactory.getActiveService().getExperiments(limit, offset);
    }

    /**
     * Retrieves details of a specific experiment.
     *
     * @param experimentId The unique identifier of the experiment.
     * @return A ResponseEntity containing the details of the specified experiment.
     */
    @GetMapping("/{experimentId}")
    public ResponseEntity<Experiment> getExperimentById(@PathVariable String experimentId) {
        return experimentServiceFactory.getActiveService().getExperimentById(experimentId);
    }

    /**
     * Retrieves all runs associated with a given experiment.
     *
     * @param experimentId The unique identifier of the experiment.
     * @return A ResponseEntity containing a list of runs associated with the
     *         experiment.
     */
    @GetMapping("/{experimentId}/runs")
    public ResponseEntity<List<Run>> getRunsForExperiment(@PathVariable String experimentId) {
        return experimentServiceFactory.getActiveService().getRunsForExperiment(experimentId);
    }

    /**
     * Retrieves details of a specific run within an experiment.
     * <p>
     * The response includes metadata, parameters, and metrics for the run.
     *
     * @param experimentId The ID of the experiment the run belongs to.
     * @param runId        The unique identifier of the run.
     * @return A ResponseEntity containing the details of the specified run.
     */
    @GetMapping("/{experimentId}/runs/{runId}")
    public ResponseEntity<Run> getRunById(
            @PathVariable String experimentId,
            @PathVariable String runId) {
        return experimentServiceFactory.getActiveService().getRunById(experimentId, runId);
    }

    /**
     * Retrieves all logged values for a specific metric within a given run.
     * <p>
     * Returns a list of recorded metric values for the requested metric.
     *
     * @param experimentId The ID of the experiment the run belongs to.
     * @param runId        The unique identifier of the run.
     * @param metricName   The name of the metric to retrieve values for.
     * @return A ResponseEntity containing a list of metric values.
     */
    @GetMapping("/{experimentId}/runs/{runId}/metrics/{metricName}")
    public ResponseEntity<List<Metric>> getMetricValues(
            @PathVariable String experimentId,
            @PathVariable String runId,
            @PathVariable String metricName) {
        return experimentServiceFactory.getActiveService().getMetricValues(experimentId, runId, metricName);
    }

    @GetMapping("/{experimentId}/runs/{runId}/metrics-all/{metricName}")
    public ResponseEntity<List<Metric>> getAllMetrics(
            @PathVariable String experimentId,
            @PathVariable String runId,
            @PathVariable String metricName) {
        return experimentServiceFactory.getActiveService().getAllMetrics(experimentId, runId, metricName);
    }

    /**
     * This endpoint abstracts user feedback submitted via the dashboard.
     * Most experiment tracking tools do not natively support such evaluations,
     * so implementations may store them using available mechanisms — such as run
     * tags, metadata, or annotations.
     * This abstraction provides a unified way to capture user input, while allowing
     * backend flexibility based on each tool’s capabilities.
     * <p>
     * Returns a list of recorded metric values for the requested metric.
     *
     * @param experimentId The ID of the experiment the run belongs to.
     * @param runId        The unique identifier of the run.
     * @return A ResponseEntity containing a list of metric values.
     */
    @PostMapping("/{experimentId}/runs/{runId}/user-evaluation")
    public ResponseEntity<UserEvaluationResponse> submitUserEvaluation(
            @PathVariable String experimentId,
            @PathVariable String runId,
            @RequestBody UserEvaluation userEvaluation) {
        return experimentServiceFactory.getActiveService().submitUserEvaluation(experimentId, runId, userEvaluation);
    }

}
