package gr.imsi.athenarc.xtremexpvisapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Experiment;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Run;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Metric;

import java.util.List;

/**
 * Defines the abstract contract for experiment tracking endpoints.
 * This abstract class should be extended for each supported tracking tool
 * (e.g., MLflow, ExtremeXP).
 * It provides endpoints to retrieve experiments, fetch specific experiment
 * details,
 * list associated runs, retrieve specific run details, and fetch recorded
 * metric values.
 */
@RestController
@RequestMapping("/experiments")
public abstract class ExperimentController {

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
    public abstract ResponseEntity<List<Experiment>> getExperiments(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset);

    /**
     * Retrieves details of a specific experiment.
     *
     * @param experimentId The unique identifier of the experiment.
     * @return A ResponseEntity containing the details of the specified experiment.
     */
    @GetMapping("/{experimentId}")
    public abstract ResponseEntity<Experiment> getExperimentById(@PathVariable String experimentId);

    /**
     * Retrieves all runs associated with a given experiment.
     *
     * @param experimentId The unique identifier of the experiment.
     * @return A ResponseEntity containing a list of runs associated with the
     *         experiment.
     */
    @GetMapping("/{experimentId}/runs")
    public abstract ResponseEntity<List<Run>> getRunsForExperiment(@PathVariable String experimentId);

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
    public abstract ResponseEntity<Run> getRunById(
            @PathVariable String experimentId,
            @PathVariable String runId);

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
    public abstract ResponseEntity<List<Metric>> getMetricValues(
            @PathVariable String experimentId,
            @PathVariable String runId,
            @PathVariable String metricName);
}
