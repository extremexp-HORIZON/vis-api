package gr.imsi.athenarc.xtremexpvisapi.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Experiment;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Run;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Metric;

import java.util.List;

/**
 * ExtremeXP implementation of the ExperimentService.
 * Connects to ExtremeXP execution engine to retrieve experiment data.
 */
@Service("extremeXP")
public class ExtremeXPExperimentService implements ExperimentService {

    @Override
    public ResponseEntity<List<Experiment>> getExperiments(int limit, int offset) {
        // Implement ExtremeXP-specific logic to retrieve experiments
        return null; // Replace with actual implementation
    }

    @Override
    public ResponseEntity<Experiment> getExperimentById(String experimentId) {
        // Implement ExtremeXP-specific logic
        return null; // Replace with actual implementation
    }

    @Override
    public ResponseEntity<List<Run>> getRunsForExperiment(String experimentId) {
        // Implement ExtremeXP-specific logic
        return null; // Replace with actual implementation
    }

    @Override
    public ResponseEntity<Run> getRunById(String experimentId, String runId) {
        // Implement ExtremeXP-specific logic
        return null; // Replace with actual implementation
    }

    @Override
    public ResponseEntity<List<Metric>> getMetricValues(String experimentId, String runId, String metricName) {
        // Implement ExtremeXP-specific logic
        return null; // Replace with actual implementation
    }
}
