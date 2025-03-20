package gr.imsi.athenarc.xtremexpvisapi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Experiment;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Run;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Metric;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * ExtremeXP implementation of the ExperimentService.
 * Connects to ExtremeXP execution engine to retrieve experiment data.
 */
@Service("extremeXP")
public class ExtremeXPExperimentService implements ExperimentService {

    @Value("${extremexp.workflowsApi.url}")
    private String workflowsApiUrl;

    @Value("${extremexp.workflowsApi.key}")
    private String workflowsApiKey;

    private final RestTemplate restTemplate;

    public ExtremeXPExperimentService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public ResponseEntity<List<Experiment>> getExperiments(int limit, int offset) {
        // Implement ExtremeXP-specific logic to retrieve experiments
        return null; // Replace with actual implementation
    }

    @Override
    public ResponseEntity<Experiment> getExperimentById(String experimentId) {
        String requestUrl = workflowsApiUrl + "/experiments/" + experimentId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("access-token", workflowsApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    requestUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return ResponseEntity.ok(mapToExperiment(response.getBody()));
            } else {
                return ResponseEntity.status(response.getStatusCode()).build();
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Experiment mapToExperiment(Map<String, Object> data) {
        Experiment experiment = new Experiment();
        // map experiment data
        return experiment;
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
