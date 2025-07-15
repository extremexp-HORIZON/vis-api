package gr.imsi.athenarc.xtremexpvisapi.service.experiment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.DataAsset;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Experiment;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Metric;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.MetricDefinition;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Param;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Run;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Task;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.UserEvaluation;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.UserEvaluationResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Run.Status;
import gr.imsi.athenarc.xtremexpvisapi.domain.lifecycle.ControlRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.lifecycle.ControlResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv2.params.SourceType;
import gr.imsi.athenarc.xtremexpvisapi.domain.reorder.ReorderRequest;
import lombok.extern.java.Log;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ExtremeXP implementation of the ExperimentService.
 * Connects to ExtremeXP execution engine to retrieve experiment data.
 */
@Service("extremeXP")
@Log
public class ExtremeXPExperimentService implements ExperimentService {

    @Value("${extremexp.workflowsApi.url}")
    private String workflowsApiUrl;

    @Value("${extremexp.workflowsApi.key}")
    private String workflowsApiKey;

    @Value("${extremexp.experimentationEngineApi.url}")
    private String experimentationEngineApiUrl;

    private final RestTemplate restTemplate;

    public ExtremeXPExperimentService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public ResponseEntity<List<Experiment>> getExperiments(int limit, int offset) {
        String requestUrl = workflowsApiUrl + "/experiments"; // API URL

        HttpEntity<String> entity = new HttpEntity<>(headersInitializer());

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    requestUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Extract the experiments list
                List<Map<String, Map<String, Object>>> experimentsList = (List<Map<String, Map<String, Object>>>) response
                        .getBody().get("experiments");

                // Convert to Experiment objects
                List<Experiment> experiments = experimentsList.stream()
                        .map(expMap -> expMap.values().iterator().next()) // Extract inner object
                        .map(this::mapToExperiment) // Convert to `Experiment` object
                        .skip(offset) // Apply offset
                        .limit(limit) // Apply limit
                        .collect(Collectors.toList()); // Collect as List

                // Iterate over each experiment to fetch metrics
                for (Experiment experiment : experiments) {
                    // Make the second API call to /metrics-query for each experiment
                    String requestUrlMetc = workflowsApiUrl + "/metrics-query";
                    Map<String, Object> requestBody = new HashMap<>();
                    requestBody.put("experimentId", experiment.getId());

                    // Create a new HttpEntity with the body for the metrics query request
                    HttpEntity<Map<String, Object>> entityForMetrics = new HttpEntity<>(requestBody,
                            headersInitializer());

                    // Second API call to fetch metrics data
                    ResponseEntity<List> responseMetrics = restTemplate.exchange(
                            requestUrlMetc,
                            HttpMethod.POST,
                            entityForMetrics,
                            List.class);

                    if (responseMetrics.getStatusCode() == HttpStatus.OK && responseMetrics.getBody() != null) {
                        List<Map<String, Object>> metricsResponse = (List<Map<String, Object>>) responseMetrics
                                .getBody();
                        List<MetricDefinition> metricDefinitions = new ArrayList<>();
                        Map<String, MetricDefinition> uniqueMetricDefinitions = new HashMap<>();

                        // Process metrics and populate MetricDefinition list
                        for (Map<String, Object> metricResponse : metricsResponse) {
                            String name = (String) metricResponse.get("name");
                            String semanticType = (String) metricResponse.get("semantic_type");

                            // Only add if semanticType is unique
                            if (!uniqueMetricDefinitions.containsKey(semanticType)) {
                                MetricDefinition metricDefinition = new MetricDefinition();
                                metricDefinition.setName(name);
                                metricDefinition.setSemanticType(semanticType);
                                metricDefinition.setGreaterIsBetter(null);
                                uniqueMetricDefinitions.put(semanticType, metricDefinition);
                            }
                        }

                        // Set the metricDefinitions to the experiment
                        // experiment.setMetricDefinitions(metricDefinitions);
                        experiment.setMetricDefinitions(new ArrayList<>(uniqueMetricDefinitions.values()));

                    } else {
                        System.out.println("Failed to retrieve metrics for experiment " + experiment.getId()
                                + ". Status Code: " + responseMetrics.getStatusCode());
                        System.out.println("Metrics Response Body: " + responseMetrics.getBody());
                    }
                }

                // Return the list of experiments with populated metrics
                return ResponseEntity.ok(experiments);
            } else {
                return ResponseEntity.status(response.getStatusCode()).build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    @Cacheable(value = "experimentCache", key = "#experimentId", unless = "#result.body == null")
    public ResponseEntity<Experiment> getExperimentById(String experimentId) {
        String requestUrl = workflowsApiUrl + "/experiments/" + experimentId;

        HttpEntity<String> entity = new HttpEntity<>(headersInitializer());

        try {
            // First API call to get experiment details
            ResponseEntity<Map> response = restTemplate.exchange(
                    requestUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class);

            // Debugging response for experiment details
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> experimentData = (Map<String, Object>) response.getBody().get("experiment");
                Experiment experiment = mapToExperiment(experimentData);

                // Debugging the experiment details

                // Now, make the second API call to /metrics-query
                String requestUrlMetc = workflowsApiUrl + "/metrics-query";
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("experimentId", experimentId);

                // Create a new HttpEntity with the body for the metrics query request
                HttpEntity<Map<String, Object>> entityForMetrics = new HttpEntity<>(requestBody, headersInitializer());

                // Second API call to fetch metrics data
                ResponseEntity<List> responseMetrics = restTemplate.exchange(
                        requestUrlMetc,
                        HttpMethod.POST,
                        entityForMetrics,
                        List.class);

                // Debugging metrics response
                if (responseMetrics.getStatusCode() == HttpStatus.OK && responseMetrics.getBody() != null) {
                    List<Map<String, Object>> metricsResponse = (List<Map<String, Object>>) responseMetrics.getBody();
                    Map<String, MetricDefinition> uniqueMetricDefinitions = new HashMap<>();

                    for (Map<String, Object> metricResponse : metricsResponse) {
                        String name = (String) metricResponse.get("name");
                        String semanticType = (String) metricResponse.get("semantic_type");

                        String producedByTask = (String) metricResponse.get("producedByTask");

                        if (!uniqueMetricDefinitions.containsKey(name)) {
                            MetricDefinition metricDefinition = new MetricDefinition();
                            metricDefinition.setName(name);
                            metricDefinition.setSemanticType(semanticType);
                            metricDefinition.setGreaterIsBetter(null);
                            metricDefinition.setProducedByTask(producedByTask);
                            uniqueMetricDefinitions.put(name, metricDefinition);
                        }
                    }
                    // Convert map values to list and set to experiment
                    experiment.setMetricDefinitions(new ArrayList<>(uniqueMetricDefinitions.values()));

                } else {
                    System.out.println("Failed to retrieve metrics. Status Code: " + responseMetrics.getStatusCode());
                    System.out.println("Metrics Response Body: " + responseMetrics.getBody());
                }

                // Return the experiment details
                return ResponseEntity.ok(experiment);
            } else {
                // Debugging experiment fetch failure
                System.out.println("Failed to fetch experiment. Status Code: " + response.getStatusCode());
                System.out.println("Experiment Fetch Response Body: " + response.getBody());
                return ResponseEntity.status(response.getStatusCode()).build();
            }
        } catch (Exception e) {
            // Log exception stack trace for more detailed error
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Cacheable(value = "runsCache", key = "'runsfor::' + #experimentId", unless = "#result.body.?[status.name() != 'COMPLETED'].size() > 0")
    public ResponseEntity<List<Run>> getRunsForExperiment(String experimentId) {
        String requestUrl = workflowsApiUrl + "/workflows-query";

        // Create request body with experimentId
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("experimentId", experimentId);

        // Create HTTP entity with headers and body
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headersInitializer());

        try {
            ResponseEntity<List> response = restTemplate.exchange(
                    requestUrl,
                    HttpMethod.POST,
                    entity,
                    List.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null
                    && response.getBody().size() > 0) {
                List<Object> responseObjects = (List<Object>) response.getBody();
                List<Run> runs = responseObjects.parallelStream()
                        .filter(Map.class::isInstance)
                        .map(run -> runPreparation((Map<String, Object>) run))
                        .collect(Collectors.toList());

                return ResponseEntity.ok(runs);
            } else {
                throw new RuntimeException("Failed to fetch runs for experiment: " + experimentId);
            }
        } catch (HttpClientErrorException | HttpServerErrorException | ResourceAccessException e) {
            throw new RuntimeException("Failed to communicate with the server: ");
        }
    }

    @Override
    @Cacheable(value = "runsCache", key = "'runfor::' + #experimentId", unless = "#result.body.getStatus().name() != 'COMPLETED'")
    public ResponseEntity<Run> getRunById(String experimentId, String runId) {
        try {
            HttpEntity<String> entity = new HttpEntity<>(headersInitializer());
            String workflowUrl = workflowsApiUrl + "/workflows/" + runId;
            ResponseEntity<Map> workflowResponse = restTemplate.exchange(
                    workflowUrl, HttpMethod.GET, entity, Map.class);

            // Extract workflow data from the response and prepare the Run object
            Map<String, Object> workflowData = (Map<String, Object>) workflowResponse.getBody().get("workflow");
            Run parsedRun = runPreparation(workflowData);
            parsedRun.setId(runId);

            return ResponseEntity.ok(parsedRun);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public ResponseEntity<List<Metric>> getMetricValues(String experimentId, String runId, String metricName) {
        String requestUrl = workflowsApiUrl + "/metrics/" + metricName; // API URL

        HttpEntity<String> entity = new HttpEntity<>(headersInitializer());
        ResponseEntity<Map> workflowResponse = restTemplate.exchange(
                requestUrl, HttpMethod.GET, entity, Map.class);
        try {
            Map<String, Object> workflowData = workflowResponse.getBody();
            if (workflowData == null) {
                return ResponseEntity.badRequest().build();
            }

            List<Metric> metrics = new ArrayList<>();
            Object rawValue = workflowData.get("value");

            String metricNameFromData = (String) workflowData.get("name");
            String producedByTask = (String) workflowData.get("producedByTask");
            // Instant timestamp = getTimestampFromWorkflowData(workflowData);

            if (rawValue instanceof String) {
                String valueStr = ((String) rawValue).trim();

                if (valueStr.startsWith("[") && valueStr.endsWith("]")) {
                    // It's a stringified list
                    try {
                        valueStr = valueStr.substring(1, valueStr.length() - 1); // remove brackets
                        String[] parts = valueStr.split(",");

                        if (parts.length > 0) {
                            String lastPart = parts[parts.length - 1].trim();
                            double val = Double.parseDouble(lastPart);

                            Metric metric = new Metric();
                            metric.setName(metricNameFromData);
                            metric.setTask(producedByTask);
                            metric.setValue(val);
                            metric.setStep(parts.length); // last one = last step
                            // metric.setTimestamp(timestamp); // uncomment if needed

                            metrics.add(metric);
                        }

                    } catch (NumberFormatException e) {
                        log.warning("Invalid number in list: " + valueStr);
                        return ResponseEntity.badRequest().build();
                    }
                } else {
                    // Single string value
                    try {
                        double val = Double.parseDouble(valueStr);
                        Metric metric = new Metric();
                        metric.setName(metricNameFromData);
                        metric.setTask(producedByTask);
                        metric.setValue(val);
                        metric.setStep(1); // Just one value = one step
                        // metric.setTimestamp(parseIsoDateToMillis(timestamp.toString()));
                        metrics.add(metric);
                    } catch (NumberFormatException e) {
                        log.warning("Invalid single value: " + valueStr);
                        return ResponseEntity.badRequest().build();
                    }
                }
            } else {
                log.warning("rawValue is not a String");
                return ResponseEntity.badRequest().build();
            }

            return ResponseEntity.ok(metrics);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public ResponseEntity<List<Metric>> getAllMetrics(String experimentId, String runId, String metricName) {
        String requestUrl = workflowsApiUrl + "/metrics-query";
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("experimentId", experimentId);
        requestBody.put("parent_id", runId);
        requestBody.put("name", metricName);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headersInitializer());
        try {
            ResponseEntity<List> response = restTemplate.exchange(
                    requestUrl, HttpMethod.POST, entity, List.class);
            List<Map<String, Object>> responseList = response.getBody();
            if (responseList == null || responseList.isEmpty()) {
                return ResponseEntity.ok(new <List<Metric>>ArrayList());
            }
            List<Metric> metrics = new ArrayList<>();
            for (Map<String, Object> workflowData : responseList) {
                List<Metric> parsedMetrics = mapToMetrics(workflowData);
                metrics.addAll(parsedMetrics);
            }

            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }

    @Override
    public ResponseEntity<UserEvaluationResponse> submitUserEvaluation(String experimentId, String runId,
            UserEvaluation userEvaluation) {
        String requestUrl = workflowsApiUrl + "/metrics-query";
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("experimentId", experimentId);
        requestBody.put("parent_id", runId);
        requestBody.put("name", "rating");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headersInitializer());
        try {
            ResponseEntity<List> response = restTemplate.exchange(
                    requestUrl, HttpMethod.POST, entity, List.class);
            List<Map<String, Object>> responseList = response.getBody();
            if (responseList == null || responseList.isEmpty()) {
                System.out.println("No metrics found for the given experiment and run.");
                String putUrl = workflowsApiUrl + "/metrics";
                Map<String, Object> putBody = new HashMap<>();
                // putBody.put("experimentId", experimentId);
                putBody.put("parent_id", runId);
                putBody.put("name", "rating");
                putBody.put("parent_type", "workflow");
                // putBody.put("semanticType", "user_rating");
                // putBody.put("step", 1);
                putBody.put("value", userEvaluation.getRating().toString());
                HttpEntity<Map<String, Object>> putEntity = new HttpEntity<>(putBody, headersInitializer());
                ResponseEntity<String> putResponse = restTemplate.exchange(
                        putUrl, HttpMethod.PUT, putEntity, String.class);
                if (putResponse.getStatusCode() == HttpStatus.OK) {
                    return ResponseEntity
                            .ok(new UserEvaluationResponse("success", "User evaluation submitted successfully"));
                } else {
                    return ResponseEntity.status(putResponse.getStatusCode()).build();
                }

                // return ResponseEntity.ok(new UserEvaluationResponse("failed", "User
                // evaluation was not submitted "));
            } else {
                Map<String, Object> metric = responseList.get(0); // assuming only one relevant metric
                String metricId = (String) metric.get("id");
                // Construct the URL
                String postUrl = workflowsApiUrl + "/metrics/" + metricId;
                // Create the body
                Map<String, Object> postBody = new HashMap<>();
                postBody.put("value", userEvaluation.getRating().toString());

                // Create the request
                HttpEntity<Map<String, Object>> postEntity = new HttpEntity<>(postBody, headersInitializer());

                // Send POST request
                ResponseEntity<String> postResponse = restTemplate.exchange(
                        postUrl, HttpMethod.POST, postEntity, String.class);
                if (postResponse.getStatusCode() == HttpStatus.OK) {
                    return ResponseEntity
                            .ok(new UserEvaluationResponse("success", "User evaluation updated successfully"));
                } else {
                    return ResponseEntity.status(postResponse.getStatusCode()).body(
                            new UserEvaluationResponse("failed", "Failed to update existing user evaluation"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public ResponseEntity<ControlResponse> controlLifeCycle(ControlRequest controlRequest) {
        String baseUrl = experimentationEngineApiUrl + "/exp/";
        String entityType = (controlRequest.getExperimentId() != null) ? "experiment/" : "workflow/";
        String entityId = (controlRequest.getExperimentId() != null) ? controlRequest.getExperimentId()
                : controlRequest.getRunId();
        String requestUrl = baseUrl + entityType + controlRequest.getAction() + "/" + entityId;

        try {
            ResponseEntity<ControlResponse> response = restTemplate.exchange(
                    requestUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headersInitializer()),
                    ControlResponse.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Helper Functions

    private HttpHeaders headersInitializer() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("access-token", workflowsApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private Run runPreparation(Map<String, Object> responseObject) {

        Run run = new Run();

        // Basic run details
        run.setId(responseObject.get("id") != null ? responseObject.get("id").toString() : null);
        run.setName(responseObject.get("name") != null ? responseObject.get("name").toString() : null);
        run.setExperimentId(
                responseObject.get("experimentId") != null ? responseObject.get("experimentId").toString() : null);
        run.setStartTime(
                responseObject.get("start") != null ? parseIsoDateToMillis(responseObject.get("start").toString())
                        : null);
        run.setEndTime(responseObject.containsKey("end") && responseObject.get("end") != null
                ? parseIsoDateToMillis(responseObject.get("end").toString())
                : null);

        // Status handling
        String statusStr = responseObject.get("status") != null ? responseObject.get("status").toString() : null;
        try {
            run.setStatus(statusStr != null ? Status.valueOf(statusStr.toUpperCase()) : Status.FAILED);
        } catch (IllegalArgumentException e) {
            run.setStatus(Status.FAILED); // Default or handle unknown status
        }

        // Extract and store metadata in tags
        Map<String, String> tags = new HashMap<>();
        if (responseObject.containsKey("metadata")) {
            Map<String, Object> metadata = (Map<String, Object>) responseObject.get("metadata");
            for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                if (entry.getValue() instanceof String) {
                    tags.put(entry.getKey(), (String) entry.getValue()); // Add metadata to tags
                } else {
                    tags.put(entry.getKey(), entry.getValue().toString()); // Convert non-string values to String
                }
            }
        }

        run.setTags(tags);

        // Handle metrics
        List<Map<String, Object>> metricsList = (List<Map<String, Object>>) responseObject.get("metrics");
        List<Metric> parsedMetrics = mapToMetricsNested(metricsList);

        run.setMetrics(parsedMetrics);

        // Handle tasks, params, and data assets
        List<Task> tasks = new ArrayList<>();
        List<Param> params = new ArrayList<>();
        List<DataAsset> dataAssets = new ArrayList<>();

        Object tasksObj = responseObject.get("tasks");
        if (tasksObj instanceof List<?>) {
            List<Map<String, Object>> tasksList = (List<Map<String, Object>>) tasksObj;

            for (Map<String, Object> taskObj : tasksList) {
                // Extract task details
                String taskName = (String) taskObj.get("name");
                String taskId = (String) taskObj.get("id");
                String taskType = (String) taskObj.get("source_code");
                Long taskStartTime = parseIsoDateToMillis((String) taskObj.get("start"));
                Long taskEndTime = parseIsoDateToMillis((String) taskObj.get("end"));
                Map<String, String> taskTags = new HashMap<>();
                String variant = null;
                Object metadataObj = taskObj.get("metadata");
                if (metadataObj instanceof Map<?, ?>) {
                    Map<String, Object> metadataMap = (Map<String, Object>) metadataObj;
                    metadataMap.forEach((key, value) -> {
                        taskTags.put(key, value.toString());
                    });
                    variant = (String) metadataMap.get("prototypical_name");
                }
                // Extract parameters for this task
                if (taskObj.containsKey("parameters")) {
                    List<Map<String, Object>> parameters = (List<Map<String, Object>>) taskObj.get("parameters");
                    for (Map<String, Object> paramObj : parameters) {
                        String paramName = (String) paramObj.get("name");
                        String paramValue = (String) paramObj.get("value");
                        if (paramName != null && paramValue != null) {
                            params.add(new Param(paramName, paramValue, taskName)); // Assign task to Param
                        }
                    }
                }

                // Create task object and add it to the list
                Task task = new Task(taskName, taskType, variant, taskStartTime, taskEndTime, taskTags, taskId);
                tasks.add(task);

                // Extract datasets for the task
                extractDatasets(taskObj, "input_datasets", DataAsset.Role.INPUT, taskName, dataAssets);
                extractDatasets(taskObj, "output_datasets", DataAsset.Role.OUTPUT, taskName, dataAssets);
            }
        }

        // Set extracted details into the Run object
        run.setParams(params);
        run.setTasks(tasks);
        run.setDataAssets(dataAssets);

        return run;
    }

    public List<Metric> mapToMetricsNested(List<Map<String, Object>> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyList();
        }
        List<Metric> allMetrics = new ArrayList<>();
        for (Map<String, Object> item : input) {
            for (Map.Entry<String, Object> entry : item.entrySet()) {
                Map<String, Object> data = (Map<String, Object>) entry.getValue();
                List<Metric> metrics = mapToMetrics(data);
                allMetrics.addAll(metrics);
            }
        }

        return allMetrics;
    }

    private List<Metric> mapToMetrics(Map<String, Object> data) {
        List<Metric> metrics = new ArrayList<>();
        String metricName = (String) data.get("name");
        String producedByTask = (String) data.get("producedByTask");
        Object rawValue = data.get("value");
        long timestamp = getTimestampFromWorkflowData(data);

        if (rawValue instanceof String) { 
            String valueStr = ((String) rawValue).trim();
            if (valueStr.startsWith("[") && valueStr.endsWith("]")) {
                try {
                    String[] parts = valueStr.substring(1, valueStr.length() - 1).split(",");
                    for (int i = 0; i < parts.length; i++) {
                        double value = Double.parseDouble(parts[i].trim());
                        int step = i + 1;
                        metrics.add(new Metric(metricName, value, timestamp, step, producedByTask));
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Invalid number in list: " + valueStr);
                    return Collections.emptyList();
                }
            } else {
                try {
                    double value = Double.parseDouble(valueStr);
                    metrics.add(new Metric(metricName, value, timestamp, 1, producedByTask));
                } catch (NumberFormatException e) {
                    System.err.println("Invalid single value: " + valueStr);
                    return Collections.emptyList();
                }
            }
        } else if (rawValue instanceof Number) {
            double value = ((Number) rawValue).doubleValue();
            int step = (Integer) data.getOrDefault("step", 1);
            metrics.add(new Metric(metricName, value, timestamp, step, producedByTask));
        } else {
            metrics.add(new Metric(metricName, 0, timestamp, 0, producedByTask));
        }

        return metrics;
    }

    private long getTimestampFromWorkflowData(Map<String, Object> workflowData) {
        Object dateObj = workflowData.get("date");
        return dateObj != null ? parseIsoDateToMillis((String) dateObj) : 0L;
    }

    private Experiment mapToExperiment(Map<String, Object> data) {
        Experiment experiment = new Experiment();
        experiment.setId((String) data.get("id"));
        experiment.setName((String) data.get("name"));
        Map<String, String> tags = new HashMap<>();
        Object metadata = data.get("metadata");
        if (metadata instanceof Map) {
            Map<String, String> metadataMap = (Map<String, String>) metadata;
            tags.putAll(metadataMap);
        }
        experiment.setTags(tags);
        experiment.setCreationTime(parseIsoDateToMillis((String) data.get("start")));
        experiment.setLastUpdateTime(parseIsoDateToMillis((String) data.get("end")));

        return experiment;
    }

    private Long parseIsoDateToMillis(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(isoDate).toEpochMilli(); // Convert ISO string to milliseconds
        } catch (DateTimeParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void extractDatasets(Map<String, Object> taskObj, String key, DataAsset.Role role, String taskName,
            List<DataAsset> dataAssets) {
        if (taskObj.containsKey(key)) {
            List<Map<String, Object>> datasetList = (List<Map<String, Object>>) taskObj.get(key);
            for (Map<String, Object> dataset : datasetList) {
                String name;
                String uri = (String) dataset.get("uri");
                Map<String, String> tags = new HashMap<>();
                String folder = null;
                SourceType sourceType = uri.contains("http") ? SourceType.http : SourceType.local;
                String format = getFileFormat(sourceType, dataset);
                //If the are multiple datasets with the same name, we will put them under a folder and use metadata/file_name as the name
                if(datasetList.stream()
                .filter(d -> d.containsKey("name") && d.get("name").equals(dataset.get("name"))).count() > 1){
                    folder = (String) dataset.get("name");
                    if(dataset.containsKey("metadata") && dataset.get("metadata") instanceof Map){
                    // If metadata is present, extract file_name from it
                    Map<String, Object> metadata = (Map<String, Object>) dataset.get("metadata");
                    name = (String) metadata.get("file_name");
                    }else{
                    name = (String) dataset.get("name");
                    }
                }else{
                    name = (String) dataset.get("name");
                }

                dataAssets.add(new DataAsset(name, sourceType, uri, format, role, taskName, tags, folder));
            }
        }
    }

    private String getFileFormat(SourceType sourceType, Map<String, Object> dataset) {
        if (sourceType == SourceType.http) {
            if(dataset.containsKey("metadata")){
                Map<String, String> metadata = (Map<String, String>) dataset.get("metadata");
                if (metadata.containsKey("file_name")) {
                    String name = metadata.get("file_name");
                    return name.lastIndexOf(".") != -1 ? name.substring(name.lastIndexOf(".")) : null;
                } else {
                    throw new IllegalArgumentException("Metadata does not contain file_name for HTTP dataset");
                }
            }else{
                throw new IllegalArgumentException("Metadata not found for HTTP dataset");
            }
        } else {
            if (dataset.containsKey("uri")) {
                String uri = (String) dataset.get("uri");
                if (uri.contains(".")) {
                    return uri.lastIndexOf(".") != -1 ? uri.substring(uri.lastIndexOf(".")) : null;
                } else {
                    log.warning("No file extension found in URI: " + uri);
                    return null;
                }
            } else {
                throw new IllegalArgumentException("URI not found for local dataset");
            }
        }
    }

    @Override
    public ResponseEntity<List<Run>> reorderWorkflows(ReorderRequest reorderRequest) {
        String requestUrl = experimentationEngineApiUrl + "/experiments-sort-workflows/"
                + reorderRequest.getExperimentId();

        Map<String, Object> requestBody = new HashMap<>();
        Map<String, String> precedence = new HashMap<>();
        precedence.put(reorderRequest.getPrecedingWorkflowId(), reorderRequest.getExperimentId());
        requestBody.put("precedence", precedence);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headersInitializer());
        try {
            ResponseEntity<List> response = restTemplate.exchange(
                    requestUrl,
                    HttpMethod.POST,
                    entity,
                    List.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null
                    && response.getBody().size() > 0) {
                List<Object> responseObjects = (List<Object>) response.getBody();
                List<Run> runs = responseObjects.parallelStream()
                        .filter(Map.class::isInstance)
                        .map(run -> runPreparation((Map<String, Object>) run))
                        .collect(Collectors.toList());

                return ResponseEntity.ok(runs);
            } else {
                return ResponseEntity.status(response.getStatusCode()).build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
