package gr.imsi.athenarc.xtremexpvisapi.service;

import org.springframework.http.ResponseEntity;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Experiment;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Run;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.UserEvaluation;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.UserEvaluationResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Metric;

import java.util.List;

/**
 * Service interface for experiment tracking operations.
 * Implementations will connect to different experiment tracking tools.
 */
public interface ExperimentService {
    ResponseEntity<List<Experiment>> getExperiments(int limit, int offset);
    ResponseEntity<Experiment> getExperimentById(String experimentId);
    ResponseEntity<List<Run>> getRunsForExperiment(String experimentId);
    ResponseEntity<Run> getRunById(String experimentId, String runId);
    ResponseEntity<List<Metric>> getMetricValues(String experimentId, String runId, String metricName);
    ResponseEntity<List<Metric>> getAllMetrics(String experimentId, String runId, String metricName);
    ResponseEntity<UserEvaluationResponse> submitUserEvaluation(String experimentId, String runId, UserEvaluation userEvaluation);
}
