package gr.imsi.athenarc.xtremexpvisapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.DataAsset;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Experiment;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Run;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.UserEvaluation;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.UserEvaluationResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Metric;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Param;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * MLflow implementation of the ExperimentService.
 * Connects to MLflow tracking server to retrieve experiment data.
 */
@Service("mlflow")
public class MLflowExperimentService implements ExperimentService {

    @Value("${mlflow.tracking.url}")
    private String mlflowTrackingUrl;

    private final RestTemplate restTemplate;

    private static final Logger LOG = LoggerFactory.getLogger(MLflowExperimentService.class);

    public MLflowExperimentService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public ResponseEntity<List<Experiment>> getExperiments(int limit, int offset) {
        String requestUrl = mlflowTrackingUrl + "/api/2.0/mlflow/experiments/search";
        List<Experiment> targetExperiments = new ArrayList<>();
        String pageToken = "";
        int skipped = 0;
        int collected = 0;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        while (collected < limit) {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("max_results", Math.min(1000, limit - collected));
            requestBody.put("page_token", pageToken);

            List<String> orderBy = new ArrayList<>();
            orderBy.add("creation_time DESC");
            requestBody.put("order_by", orderBy);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            try {
                LOG.info("Sending experiments request to MLflow: {}", requestUrl);
                ResponseEntity<Map> response = restTemplate.exchange(
                        requestUrl,
                        HttpMethod.POST,
                        entity,
                        Map.class);
                LOG.info("Received response from MLflow: {}", response.getStatusCode());

                if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                    return ResponseEntity.status(response.getStatusCode()).build();
                }

                Map<String, Object> responseBody = response.getBody();
                LOG.info("Attempting to parse the received list of experiments.");
                List<Experiment> pageExperiments = mapToExperiments(responseBody);
                LOG.info("Experiments parsed successfully.");
                
                if (pageExperiments == null || pageExperiments.isEmpty()) {
                    break;
                }

                LOG.info("Starting paging handling. Total experiments received in this round: {}", pageExperiments.size());
                // Handle offset and collect only needed experiments
                for (Experiment exp : pageExperiments) {
                    if (skipped < offset) {
                        skipped++;
                        continue;
                    }
                    targetExperiments.add(exp);
                    collected++;
                    if (collected == limit) break;
                }

                if (collected == limit) break;

                pageToken = (String) responseBody.get("next_page_token");
                if (pageToken == null || pageToken.isEmpty()) {
                    break;
                }

            } catch (Exception e) {
                LOG.error("An error was encountered while fetching and/or parsing experiments list.", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }

        return ResponseEntity.ok(targetExperiments);
    }

    @Override
    public ResponseEntity<Experiment> getExperimentById(String experimentId) {
        String requestUrl = mlflowTrackingUrl + "/api/2.0/mlflow/experiments/get?experiment_id=" + experimentId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            LOG.info("Sending experiment request to MLflow for experiment with id {}: {}", experimentId, requestUrl);
            ResponseEntity<Map> response = restTemplate.exchange(
                    requestUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class);
            LOG.info("Received response from MLflow: {}", response.getStatusCode());

            Map<String, Object> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null) {
                Map<String, Object> experimentData = (Map<String, Object>) responseBody.get("experiment");
                return ResponseEntity.ok(mapToExperiment(experimentData));
            } else {
                return ResponseEntity.status(response.getStatusCode()).build();
            }

        } catch (Exception e) {
            LOG.error("An error was encountered while fetching and/or parsing experiment.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public ResponseEntity<List<Run>> getRunsForExperiment(String experimentId) {
        String requestUrl = mlflowTrackingUrl + "/api/2.0/mlflow/runs/search";
        List<Run> allRuns = new ArrayList<>();
        String pageToken = "";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        while (true) {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("experiment_ids", List.of(experimentId));
            requestBody.put("max_results", 1000);  // MLflow maximum page size
            requestBody.put("page_token", pageToken);
            requestBody.put("order_by", List.of("start_time DESC"));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            try {
                LOG.info("Sending runs request to MLflow for experiment with id {}: {}", experimentId, requestUrl);
                ResponseEntity<Map> response = restTemplate.exchange(
                    requestUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class);
                LOG.info("Received response from MLflow: {}", response.getStatusCode());

                if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                    return ResponseEntity.status(response.getStatusCode()).build();
                }

                Map<String, Object> responseBody = response.getBody();
                LOG.info("Attempting to parse the received list of runs.");
                List<Run> pageRuns = mapToRuns(responseBody);
                LOG.info("Runs parsed successfully.");

                if (pageRuns != null && !pageRuns.isEmpty()) {
                    allRuns.addAll(pageRuns);
                    LOG.info("Added {} runs. Total runs collected: {}", pageRuns.size(), allRuns.size());
                }

                pageToken = (String) responseBody.get("next_page_token");
                if (pageToken == null || pageToken.isEmpty()) {
                    break; // No more pages to fetch
                }

            } catch (Exception e) {
                LOG.error("Error fetching runs for experiment {}", experimentId, e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }

        return ResponseEntity.ok(allRuns);
    }

    @Override
    public ResponseEntity<Run> getRunById(String experimentId, String runId) {
        String requestUrl = mlflowTrackingUrl + "/api/2.0/mlflow/runs/get?run_id=" + runId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            LOG.info("Sending run request to MLflow for run with id {}: {}", runId, requestUrl);
            ResponseEntity<Map> response = restTemplate.exchange(
                    requestUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class);
            LOG.info("Received response from MLflow: {}", response.getStatusCode());

            Map<String, Object> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null) {
                Map<String, Object> runData = (Map<String, Object>) responseBody.get("run");
                LOG.info("Parsing run data.");
                Run run = mapToRun(runData);
                LOG.info("Run parsed successfully.");
                return ResponseEntity.ok(run);
            } else {
                return ResponseEntity.status(response.getStatusCode()).build();
            }

        } catch (Exception e) {
            LOG.error("Error fetching run with id {}", runId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public ResponseEntity<List<Metric>> getMetricValues(String experimentId, String runId, String metricName) {
        String requestUrl = mlflowTrackingUrl + "/api/2.0/mlflow/metrics/get-history?run_id=" + runId + "&metric_key=" + metricName;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            LOG.info("Sending metric request to MLflow for metric {} in run with id {}: {}", metricName, runId, requestUrl);
            ResponseEntity<Map> response = restTemplate.exchange(
                    requestUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class);
            LOG.info("Received response from MLflow: {}", response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return ResponseEntity.ok(mapMetricHistory(response.getBody()));
            } else {
                return ResponseEntity.status(response.getStatusCode()).build();
            }

        } catch (Exception e) {
            LOG.error("Error fetching metric history for metric {} in run with id {}", metricName, runId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public ResponseEntity<UserEvaluationResponse> submitUserEvaluation(String experimentId, String runId, UserEvaluation userEvaluation) {
        String requestUrl = mlflowTrackingUrl + "/api/2.0/mlflow/runs/set-tag";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            // Required field
            setRunTag(requestUrl, runId, "user_evaluation.user", userEvaluation.getUser(), headers);
            
            // Optional fields
            if (userEvaluation.getRating() != null) {
                setRunTag(requestUrl, runId, "user_evaluation.rating", 
                        String.valueOf(userEvaluation.getRating()), headers);
            }
            if (userEvaluation.getFavorite() != null) {
                setRunTag(requestUrl, runId, "user_evaluation.favorite", 
                        String.valueOf(userEvaluation.getFavorite()), headers);
            }
            if (userEvaluation.getComment() != null) {
                setRunTag(requestUrl, runId, "user_evaluation.comment", 
                        userEvaluation.getComment(), headers);
            }

            return ResponseEntity.ok(new UserEvaluationResponse());

        } catch (Exception e) {
            LOG.error("Error submitting user evaluation for run {}", runId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public ResponseEntity<List<Metric>> getAllMetrics(String experimentId, String runId, String metricName) {
        String requestUrl = mlflowTrackingUrl + "/api/2.0/mlflow/metrics/get-history?run_id=" + runId + "&metric_key=" + metricName;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            LOG.info("Sending metric request to MLflow for metric {} in run with id {}: {}", metricName, runId, requestUrl);
            ResponseEntity<Map> response = restTemplate.exchange(
                    requestUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class);
            LOG.info("Received response from MLflow: {}", response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return ResponseEntity.ok(mapMetricHistory(response.getBody()));
            } else {
                return ResponseEntity.status(response.getStatusCode()).build();
            }

        } catch (Exception e) {
            LOG.error("Error fetching metric history for metric {} in run with id {}", metricName, runId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    private void setRunTag(String requestUrl, String runId, String key, String value, HttpHeaders headers) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("run_id", runId);
        requestBody.put("key", key);
        requestBody.put("value", value);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        LOG.info("Setting tag {} for run {}", key, runId);
        ResponseEntity<Map> response = restTemplate.exchange(
                requestUrl,
                HttpMethod.POST,
                entity,
                Map.class);
        LOG.info("Received response from MLflow: {}", response.getStatusCode());

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Failed to set tag " + key + " for run " + runId);
        }
    }

    private List<Experiment> mapToExperiments(Map<String, Object> data) {
        List<Map<String, Object>> experiments = (List<Map<String, Object>>) data.get("experiments");
        if (experiments == null) return new ArrayList<>();
        
        return experiments.stream()
            .map(this::mapToExperiment)
            .collect(Collectors.toList());
    }

    private Experiment mapToExperiment(Map<String, Object> data) {
        Experiment experiment = new Experiment();
        experiment.setId((String) data.get("experiment_id"));
        experiment.setName((String) data.get("name"));
        
        // Timestamps are already in milliseconds
        Object creationTime = data.get("creation_time");
        if (creationTime != null) {
            experiment.setCreationTime(((Number) creationTime).longValue());
        }
        
        Object lastUpdateTime = data.get("last_update_time");
        if (lastUpdateTime != null) {
            experiment.setLastUpdateTime(((Number) lastUpdateTime).longValue());
        }
        
        // Map tags
        Map<String, String> tags = new HashMap<>();
        if (data.get("tags") instanceof List) {
            List<Map<String, Object>> mlflowTags = (List<Map<String, Object>>) data.get("tags");
            mlflowTags.forEach(tag -> 
                tags.put((String) tag.get("key"), (String) tag.get("value")));
        }
        experiment.setTags(tags);

        return experiment;
    }

    private List<Run> mapToRuns(Map<String, Object> data) {
        List<Map<String, Object>> runs = (List<Map<String, Object>>) data.get("runs");
        if (runs == null) return new ArrayList<>();
        
        return runs.stream()
            .map(this::mapToRun)
            .collect(Collectors.toList());
    }

    private Run mapToRun(Map<String, Object> data) {
        Map<String, Object> info = (Map<String, Object>) data.get("info");
        Map<String, Object> data2 = (Map<String, Object>) data.get("data");
        
        Run run = new Run();
        run.setId((String) info.get("run_id"));
        run.setName((String) info.get("run_name"));
        run.setExperimentId((String) info.get("experiment_id"));
        run.setStatus(mapStatus((String) info.get("status")));
        
        // Timestamps are already in milliseconds
        Object startTime = info.get("start_time");
        if (startTime != null) {
            run.setStartTime(((Number) startTime).longValue());
        }
        
        Object endTime = info.get("end_time");
        if (endTime != null) {
            run.setEndTime(((Number) endTime).longValue());
        }
        
        // Map parameters
        if (data2.get("params") instanceof List) {
            List<Map<String, Object>> params = (List<Map<String, Object>>) data2.get("params");
            run.setParams(
                params.stream()
                .map(p -> new Param((String) p.get("key"), (String) p.get("value"), null))
                .collect(Collectors.toList())
            );
        }
        
        // Map metrics
        if (data2.get("metrics") instanceof List) {
            List<Map<String, Object>> metrics = (List<Map<String, Object>>) data2.get("metrics");
            run.setMetrics(
                metrics.stream()
                .map(m -> new Metric(
                    (String) m.get("key"),
                    (Double) m.get("value"),
                    ((Number) m.get("timestamp")).longValue(),
                    (Integer) m.get("step"),
                    (String) m.get("producedByTask"))
                )
                .collect(Collectors.toList()));
        }

        // Map inputs
        if (data.get("inputs") != null) {
            Map<String, Object> inputs = (Map<String, Object>) data.get("inputs");
            if (inputs.get("dataset_inputs") instanceof List) {
                List<Map<String, Object>> datasetInputs = (List<Map<String, Object>>) inputs.get("dataset_inputs");
                run.setDataAssets(
                    datasetInputs.stream()
                    .map(this::mapToDataAsset)
                    .collect(Collectors.toList())
                );
            }
        }
        
        // Map tags
        Map<String, String> tags = new HashMap<>();
        if (data2.get("tags") instanceof List) {
            List<Map<String, Object>> mlflowTags = (List<Map<String, Object>>) data2.get("tags");
            mlflowTags.forEach(tag -> 
                tags.put((String) tag.get("key"), (String) tag.get("value")));
        }
        run.setTags(tags);
        
        return run;
    }

    private DataAsset mapToDataAsset(Map<String, Object> datasetInput) {
        Map<String, Object> dataset = (Map<String, Object>) datasetInput.get("dataset");
        List<Map<String, Object>> inputTags = (List<Map<String, Object>>) datasetInput.get("tags");
        
        DataAsset asset = new DataAsset();
        asset.setName((String) dataset.get("name"));
        asset.setSourceType((String) dataset.get("source_type"));

        Object source = dataset.get("source");
        if (! (source instanceof String)) {
            throw new RuntimeException("Dataset source is not a string as specified in MLflow API: " + source);
        }
        String source_str = (String) source;
        ObjectMapper objectMapper = new ObjectMapper();
        String asset_source;
        try {
            JsonNode jsonNode = objectMapper.readTree(source_str);
            // read source, which lies in either "uri" or "url" field
            // depending on the source type
            if (jsonNode.get("uri") != null) {
                asset_source = jsonNode.get("uri").asText();
            } else if (jsonNode.get("url") != null) {
                asset_source = jsonNode.get("url").asText();
            } else {
                throw new RuntimeException("Dataset source does not contain 'uri' or 'url' field: " + source_str);
            }
        } catch (JsonProcessingException e) {
            // If the source is not a valid JSON string, we use it as is
            LOG.warn(source_str + " is not a valid JSON string. It will be used as is.");
            e.printStackTrace();
            asset_source = source_str;
        } catch (Exception e) {
            // If the source IS a valid JSON string, but it does not contain "uri" or "url" field
            // we again use the source as is (because a string is required) and log a warning
            LOG.warn(source_str + " does not contain 'uri' or 'url' field. It will be used as is.");
            e.printStackTrace();
            asset_source = source_str;
        }
        asset.setSource(asset_source);
        
        // Map tags from both dataset metadata and input tags
        Map<String, String> tags = new HashMap<>();
        
        // Add dataset metadata as tags
        if (dataset.get("digest") != null) {
            tags.put("digest", (String) dataset.get("digest"));
        }
        if (dataset.get("schema") != null) {
            tags.put("schema", (String) dataset.get("schema"));
        }
        if (dataset.get("profile") != null) {
            tags.put("profile", (String) dataset.get("profile"));
        }
        
        // Add input tags
        if (inputTags != null) {
            inputTags.forEach(tag -> 
                tags.put((String) tag.get("key"), (String) tag.get("value"))
            );
        }
        
        asset.setTags(tags);
        asset.setRole(DataAsset.Role.INPUT); // These are always input datasets from MLflow
        
        return asset;
    }

    private Run.Status mapStatus(String mlflowStatus) {
        switch (mlflowStatus.toUpperCase()) {
            case "RUNNING": return Run.Status.RUNNING;
            case "SCHEDULED": return Run.Status.SCHEDULED;
            case "FINISHED": return Run.Status.COMPLETED;
            case "FAILED": return Run.Status.FAILED;
            case "KILLED": return Run.Status.KILLED;
            default: return Run.Status.STOPPED;
        }
    }

    private List<Metric> mapMetricHistory(Map<String, Object> data) {
        List<Map<String, Object>> metrics = (List<Map<String, Object>>) data.get("metrics");
        if (metrics == null) return new ArrayList<>();
        
        return metrics.stream()
            .map(m -> new Metric(
                (String) m.get("key"),
                (Double) m.get("value"),
                ((Number) m.get("timestamp")).longValue(), // Timestamp is already in milliseconds
                (Integer) m.get("step"),
                (String) m.get("producedByTask"))
            )
            .collect(Collectors.toList());
    }

    private Metric mapToMetric(Map<String, Object> data) {
        List<Metric> metrics = mapMetricHistory(data);
        if (metrics.isEmpty()) {
            return null;
        }
        // Return the latest metric value
        return metrics.get(metrics.size() - 1);
    }
}
