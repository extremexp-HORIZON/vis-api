// package gr.imsi.athenarc.xtremexpvisapi.service;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;

// import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.DataAsset;
// import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Experiment;
// import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Run;
// import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Run.Status;
// import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Task;
// import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.UserEvaluation;
// import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Metric;
// import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.MetricDefinition;
// import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Param;

// import org.springframework.http.*;
// import org.springframework.web.client.RestTemplate;

// import java.time.Instant;
// import java.time.format.DateTimeParseException;
// import java.util.ArrayList;
// import java.util.Comparator;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.stream.Collectors;

// /**
//  * ExtremeXP implementation of the ExperimentService.
//  * Connects to ExtremeXP execution engine to retrieve experiment data.
//  */
// @Service("extremeXP")
// public class ExtremeXPExperimentService implements ExperimentService {

//     @Value("${extremexp.workflowsApi.url}")
//     private String workflowsApiUrl;

//     @Value("${extremexp.workflowsApi.key}")
//     private String workflowsApiKey;

//     private final RestTemplate restTemplate;

//     private Experiment mapToExperiment(Map<String, Object> data) {
//         Experiment experiment = new Experiment();
//         experiment.setId((String) data.get("id"));
//         experiment.setName((String) data.get("name"));
//         Map<String, String> tags = new HashMap<>();
//         Object metadata = data.get("metadata");
//         if (metadata instanceof Map) {
//             Map<String, String> metadataMap = (Map<String, String>) metadata;
//             tags.putAll(metadataMap);
//         }
//         experiment.setTags(tags);
//         experiment.setCreationTime(parseIsoDateToMillis((String) data.get("start")));
//         experiment.setLastUpdateTime(parseIsoDateToMillis((String) data.get("end")));

//         return experiment;
//     }

//     private Long parseIsoDateToMillis(String isoDate) {
//         if (isoDate == null || isoDate.isEmpty()) {
//             return null;
//         }
//         try {
//             return Instant.parse(isoDate).toEpochMilli(); // Convert ISO string to milliseconds
//         } catch (DateTimeParseException e) {
//             e.printStackTrace();
//             return null;
//         }
//     }

//     private void extractDatasets(Map<String, Object> taskObj, String key, DataAsset.Role role, String taskName,
//             List<DataAsset> dataAssets) {
//         if (taskObj.containsKey(key)) {
//             List<Map<String, Object>> datasetList = (List<Map<String, Object>>) taskObj.get(key);
//             for (Map<String, Object> dataset : datasetList) {
//                 String name = (String) dataset.get("name");
//                 String uri = (String) dataset.get("uri");
//                 Map<String, String> tags = new HashMap<>();

//                 if (dataset.containsKey("metadata")) {
//                     Map<String, Object> metadata = (Map<String, Object>) dataset.get("metadata");
//                     metadata.forEach((metaKey, metaValue) -> tags.put(metaKey, metaValue.toString()));
//                 }

//                 dataAssets.add(new DataAsset(name, "unknown", uri, "unknown", role, taskName, tags));
//             }
//         }
//     }

//     public ExtremeXPExperimentService(RestTemplate restTemplate) {
//         this.restTemplate = restTemplate;
//     }

//     @Override
//     public ResponseEntity<List<Experiment>> getExperiments(int limit, int offset) {
//         String requestUrl = workflowsApiUrl + "/experiments"; // API URL
//         HttpHeaders headers = new HttpHeaders();
//         headers.set("access-token", workflowsApiKey); // API Key
//         headers.setContentType(MediaType.APPLICATION_JSON);

//         HttpEntity<String> entity = new HttpEntity<>(headers);

//         try {
//             ResponseEntity<Map> response = restTemplate.exchange(
//                     requestUrl,
//                     HttpMethod.GET,
//                     entity,
//                     Map.class);

//             if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
//                 // Extract the experiments list
//                 List<Map<String, Map<String, Object>>> experimentsList = (List<Map<String, Map<String, Object>>>) response
//                         .getBody().get("experiments");

//                 // Convert to Experiment objects
//                 List<Experiment> experiments = experimentsList.stream()
//                         .map(expMap -> expMap.values().iterator().next()) // Extract inner object
//                         .map(this::mapToExperiment) // Convert to `Experiment` object
//                         .skip(offset) // Apply offset
//                         .limit(limit) // Apply limit
//                         .collect(Collectors.toList()); // Collect as List

//                 // Iterate over each experiment to fetch metrics
//                 for (Experiment experiment : experiments) {
//                     // Make the second API call to /metrics-query for each experiment
//                     String requestUrlMetc = workflowsApiUrl + "/metrics-query";
//                     Map<String, Object> requestBody = new HashMap<>();
//                     requestBody.put("experimentId", experiment.getId());

//                     // Create a new HttpEntity with the body for the metrics query request
//                     HttpEntity<Map<String, Object>> entityForMetrics = new HttpEntity<>(requestBody, headers);

//                     // Second API call to fetch metrics data
//                     ResponseEntity<List> responseMetrics = restTemplate.exchange(
//                             requestUrlMetc,
//                             HttpMethod.POST,
//                             entityForMetrics,
//                             List.class);

//                     if (responseMetrics.getStatusCode() == HttpStatus.OK && responseMetrics.getBody() != null) {
//                         List<Map<String, Object>> metricsResponse = (List<Map<String, Object>>) responseMetrics
//                                 .getBody();
//                         List<MetricDefinition> metricDefinitions = new ArrayList<>();
//                         Map<String, MetricDefinition> uniqueMetricDefinitions = new HashMap<>();

//                         // Process metrics and populate MetricDefinition list
//                         for (Map<String, Object> metricResponse : metricsResponse) {
//                             String name = (String) metricResponse.get("name");
//                             String semanticType = (String) metricResponse.get("semantic_type");

//                             // Only add if semanticType is unique
//                             if (!uniqueMetricDefinitions.containsKey(semanticType)) {
//                                 MetricDefinition metricDefinition = new MetricDefinition();
//                                 metricDefinition.setName(name);
//                                 metricDefinition.setSemanticType(semanticType);
//                                 metricDefinition.setDescription("Description not available");
//                                 metricDefinition.setUnit("unit not available");
//                                 metricDefinition.setGreaterIsBetter(null); // Set based on your logic

//                                 uniqueMetricDefinitions.put(semanticType, metricDefinition);
//                             }
//                         }

//                         // Set the metricDefinitions to the experiment
//                         // experiment.setMetricDefinitions(metricDefinitions);
//                         experiment.setMetricDefinitions(new ArrayList<>(uniqueMetricDefinitions.values()));

//                     } else {
//                         System.out.println("Failed to retrieve metrics for experiment " + experiment.getId()
//                                 + ". Status Code: " + responseMetrics.getStatusCode());
//                         System.out.println("Metrics Response Body: " + responseMetrics.getBody());
//                     }
//                 }

//                 // Return the list of experiments with populated metrics
//                 return ResponseEntity.ok(experiments);
//             } else {
//                 return ResponseEntity.status(response.getStatusCode()).build();
//             }
//         } catch (Exception e) {
//             e.printStackTrace();
//             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//         }
//     }

//     @Override
//     public ResponseEntity<Experiment> getExperimentById(String experimentId) {
//         String requestUrl = workflowsApiUrl + "/experiments/" + experimentId;
//         HttpHeaders headers = new HttpHeaders();
//         headers.set("access-token", workflowsApiKey);
//         headers.setContentType(MediaType.APPLICATION_JSON);

//         HttpEntity<String> entity = new HttpEntity<>(headers);

//         try {
//             // First API call to get experiment details
//             ResponseEntity<Map> response = restTemplate.exchange(
//                     requestUrl,
//                     HttpMethod.GET,
//                     entity,
//                     Map.class);

//             // Debugging response for experiment details
//             if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
//                 Map<String, Object> experimentData = (Map<String, Object>) response.getBody().get("experiment");
//                 Experiment experiment = mapToExperiment(experimentData);

//                 // Debugging the experiment details

//                 // Now, make the second API call to /metrics-query
//                 String requestUrlMetc = workflowsApiUrl + "/metrics-query";
//                 Map<String, Object> requestBody = new HashMap<>();
//                 requestBody.put("experimentId", experimentId);

//                 // Create a new HttpEntity with the body for the metrics query request
//                 HttpEntity<Map<String, Object>> entityForMetrics = new HttpEntity<>(requestBody, headers);

//                 // Second API call to fetch metrics data
//                 ResponseEntity<List> responseMetrics = restTemplate.exchange(
//                         requestUrlMetc,
//                         HttpMethod.POST,
//                         entityForMetrics,
//                         List.class);

//                 // Debugging metrics response
//                 if (responseMetrics.getStatusCode() == HttpStatus.OK && responseMetrics.getBody() != null) {
//                     List<Map<String, Object>> metricsResponse = (List<Map<String, Object>>) responseMetrics.getBody();
//                     Map<String, MetricDefinition> uniqueMetricDefinitions = new HashMap<>();

//                     for (Map<String, Object> metricResponse : metricsResponse) {
//                         String name = (String) metricResponse.get("name");
//                         String semanticType = (String) metricResponse.get("semantic_type");

//                         String producedByTask = (String) metricResponse.get("producedByTask");

//                         if (!uniqueMetricDefinitions.containsKey(name)) {
//                             MetricDefinition metricDefinition = new MetricDefinition();
//                             metricDefinition.setName(name);
//                             metricDefinition.setSemanticType(semanticType);
//                             metricDefinition.setDescription("Description not available");
//                             metricDefinition.setUnit("unit not available");
//                             metricDefinition.setGreaterIsBetter(null);
//                             metricDefinition.setProducedByTask(producedByTask);
//                             uniqueMetricDefinitions.put(name, metricDefinition);
//                         }
//                     }
//                     // Convert map values to list and set to experiment
//                     experiment.setMetricDefinitions(new ArrayList<>(uniqueMetricDefinitions.values()));

//                 } else {
//                     System.out.println("Failed to retrieve metrics. Status Code: " + responseMetrics.getStatusCode());
//                     System.out.println("Metrics Response Body: " + responseMetrics.getBody());
//                 }

//                 // Return the experiment details
//                 return ResponseEntity.ok(experiment);
//             } else {
//                 // Debugging experiment fetch failure
//                 System.out.println("Failed to fetch experiment. Status Code: " + response.getStatusCode());
//                 System.out.println("Experiment Fetch Response Body: " + response.getBody());
//                 return ResponseEntity.status(response.getStatusCode()).build();
//             }
//         } catch (Exception e) {
//             // Log exception stack trace for more detailed error
//             e.printStackTrace();
//             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//         }
//     }

//     @Override
//     public ResponseEntity<List<Run>> getRunsForExperiment(String experimentId) {
//         String requestUrl = workflowsApiUrl + "/experiments/" + experimentId;
//         HttpHeaders headers = new HttpHeaders();
//         headers.set("access-token", workflowsApiKey);
//         headers.setContentType(MediaType.APPLICATION_JSON);
//         HttpEntity<String> entity = new HttpEntity<>(headers);

//         try {
//             ResponseEntity<Map> response = restTemplate.exchange(
//                     requestUrl,
//                     HttpMethod.GET,
//                     entity,
//                     Map.class);

//             if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
//                 Map<String, Object> experimentData = (Map<String, Object>) response.getBody().get("experiment");
//                 // System.out.println("experimentData: " + experimentData.get("workflow_ids"));

//                 List<String> workflowIds = (List<String>) experimentData.get("workflow_ids");
//                 List<Run> runs = new ArrayList<>();

//                 for (String runId : workflowIds) {
//                     ResponseEntity<Run> runResponse = getRunById(experimentId, runId);
//                     if (runResponse.getStatusCode() == HttpStatus.OK && runResponse.getBody() != null) {
//                         runs.add(runResponse.getBody());
//                     }
//                 }

//                 return ResponseEntity.ok(runs);
//             } else {
//                 return ResponseEntity.status(response.getStatusCode()).build();
//             }
//         } catch (Exception e) {
//             e.printStackTrace();
//             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//         }
//     }

//     @Override
//     public ResponseEntity<Run> getRunById(String experimentId, String runId) {
//         // Step 1: Get experiment details
//         HttpHeaders headers = new HttpHeaders();
//         headers.set("access-token", workflowsApiKey);
//         headers.setContentType(MediaType.APPLICATION_JSON);
//         HttpEntity<String> entity = new HttpEntity<>(headers);

//         String workflowUrl = workflowsApiUrl + "/workflows/" + runId;
//         ResponseEntity<Map> workflowResponse = restTemplate.exchange(
//                 workflowUrl, HttpMethod.GET, entity, Map.class);

//         Map<String, Object> workflowData = (Map<String, Object>) workflowResponse.getBody().get("workflow");
//         // Step 4: Convert response to Run object
//         Run run = new Run();
//         run.setId(runId);
//         run.setName((String) workflowData.get("name"));
//         run.setExperimentId((String) experimentId);
//         run.setStartTime(parseIsoDateToMillis((String) workflowData.get("start")));
//         run.setEndTime(parseIsoDateToMillis((String) workflowData.get("end")));
//         String statusStr = (String) workflowData.get("status"); // Get status as string
//         try {
//             run.setStatus(Status.valueOf(statusStr.toUpperCase())); // Convert to Enum
//         } catch (IllegalArgumentException e) {
//             run.setStatus(Status.FAILED); // Default or handle unknown status
//         }
//         // Extract and store metadata in tags
//         Map<String, String> tags = new HashMap<>();
//         if (workflowData.containsKey("metadata")) {
//             Map<String, Object> metadata = (Map<String, Object>) workflowData.get("metadata");
//             for (Map.Entry<String, Object> entry : metadata.entrySet()) {
//                 if (entry.getValue() instanceof String) {
//                     tags.put(entry.getKey(), (String) entry.getValue()); // Add metadata to tags
//                 } else {
//                     tags.put(entry.getKey(), entry.getValue().toString()); // Convert non-string values to String
//                 }
//             }
//         }

//         // Set only metadata tags to the Run object
//         run.setTags(tags);
//         ResponseEntity<Experiment> experiment = getExperimentById(experimentId);
//         List<MetricDefinition> metricdef = experiment.getBody().getMetricDefinitions();
//         List<String> metricNames = metricdef.stream()
//                 .map(MetricDefinition::getName) // Assuming there's a getName() method in MetricDefinition
//                 .collect(Collectors.toList());

//         List<Metric> finalMetrics = new ArrayList<>();

//         for (String metricId : metricNames) {
//             ResponseEntity<List<Metric>> metricResponse = getMetricValues(experimentId, runId, metricId);
//             List<Metric> fetchedMetrics = metricResponse.getBody();

//             if (fetchedMetrics == null || fetchedMetrics.isEmpty()) {
//                 continue;
//             }

//             // Case 1: Single-value metric (step is null)
//             if (fetchedMetrics.size() == 1 && fetchedMetrics.get(0).getStep() == null) {
//                 finalMetrics.add(fetchedMetrics.get(0));
//             }
//             // Case 2: Multi-step metric - pick the one with the highest step
//             else {
//                 Metric highestStepMetric = fetchedMetrics.stream()
//                         .filter(m -> m.getStep() != null)
//                         .max(Comparator.comparingInt(Metric::getStep))
//                         .orElse(null);

//                 if (highestStepMetric != null) {
//                     finalMetrics.add(highestStepMetric);
//                 }
//             }
//         }

//         run.setMetrics(finalMetrics);

//         List<Task> tasks = new ArrayList<>();
//         List<Param> params = new ArrayList<>();
//         List<DataAsset> dataAssets = new ArrayList<>();

//         Object tasksObj = workflowData.get("tasks");
//         if (tasksObj instanceof List<?>) {
//             List<Map<String, Object>> tasksList = (List<Map<String, Object>>) tasksObj;

//             for (Map<String, Object> taskObj : tasksList) {
//                 // Extract task details
//                 String taskName = (String) taskObj.get("name");
//                 String taskType = (String) taskObj.get("source_code");
//                 Long taskStartTime = parseIsoDateToMillis((String) taskObj.get("start"));
//                 Long taskEndTime = parseIsoDateToMillis((String) taskObj.get("end"));

//                 Map<String, String> taskTags = new HashMap<>();
//                 String variant = null;
//                 Object metadataObj = taskObj.get("metadata");
//                 if (metadataObj instanceof Map<?, ?>) {
//                     Map<String, Object> metadataMap = (Map<String, Object>) metadataObj;
//                     variant = (String) metadataMap.get("prototypical_name");
//                 }
//                 // Extract parameters for this task
//                 if (taskObj.containsKey("parameters")) {
//                     List<Map<String, Object>> parameters = (List<Map<String, Object>>) taskObj.get("parameters");
//                     for (Map<String, Object> paramObj : parameters) {
//                         String paramName = (String) paramObj.get("name");
//                         String paramValue = (String) paramObj.get("value");
//                         if (paramName != null && paramValue != null) {
//                             params.add(new Param(paramName, paramValue, taskName)); // Assign task to Param
//                         }
//                     }
//                 }

//                 // Create task object and add it to the list
//                 Task task = new Task(taskName, taskType, variant, taskStartTime, taskEndTime, taskTags);
//                 tasks.add(task);

//                 // Extract datasets for the task
//                 extractDatasets(taskObj, "input_datasets", DataAsset.Role.INPUT, taskName, dataAssets);
//                 extractDatasets(taskObj, "output_datasets", DataAsset.Role.OUTPUT, taskName, dataAssets);
//             }
//         }

//         // Set extracted details into the Run object
//         run.setParams(params); // Now Params have task association
//         run.setTasks(tasks);
//         run.setDataAssets(dataAssets);

//         return ResponseEntity.ok(run);

//     }

//     @Override
//     public ResponseEntity<List<Metric>> getMetricValues(String experimentId, String runId, String metricName) {
//         String requestUrl = workflowsApiUrl + "/metrics-query";

//         HttpHeaders headers = new HttpHeaders();
//         headers.set("access-token", workflowsApiKey);
//         headers.setContentType(MediaType.APPLICATION_JSON);

//         // Build request body
//         Map<String, Object> requestBody = new HashMap<>();
//         requestBody.put("experimentId", experimentId);
//         requestBody.put("parent_id", runId); // parent_id = runId
//         requestBody.put("name", metricName); // name = metricName

//         HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

//         try {
//             ResponseEntity<List> response = restTemplate.exchange(
//                     requestUrl, HttpMethod.POST, entity, List.class);

//             List<Map<String, Object>> responseList = response.getBody();
//             if (responseList == null || responseList.isEmpty()) {
//                 return ResponseEntity.badRequest().build();
//             }

//             List<Metric> metrics = new ArrayList<>();

//             for (Map<String, Object> data : responseList) {
//                 Object rawValue = data.get("value");

//                 if (rawValue instanceof Number) {
//                     Metric metric = new Metric();
//                     metric.setName((String) data.get("name"));
//                     metric.setTask((String) data.get("producedByTask"));
//                     metric.setValue(((Number) rawValue).doubleValue());
//                     metric.setTimestamp(getTimestampFromWorkflowData(data));
//                     metrics.add(metric);
//                 } else if (rawValue instanceof String) {
//                     String valueStr = ((String) rawValue).trim();
//                     if (valueStr.startsWith("[") && valueStr.endsWith("]")) {
//                         valueStr = valueStr.substring(1, valueStr.length() - 1);
//                         String[] parts = valueStr.split(",");
//                         for (int i = 0; i < 10; i++) {
//                             try {
//                                 double val = Double.parseDouble(parts[i].trim());
//                                 Metric metric = new Metric();
//                                 metric.setName((String) data.get("name"));
//                                 metric.setTask((String) data.get("producedByTask"));
//                                 metric.setValue(val);
//                                 metric.setStep(i);
//                                 metric.setTimestamp(getTimestampFromWorkflowData(data));
//                                 metrics.add(metric);
//                             } catch (NumberFormatException e) {
//                                 System.err.println("Skipping invalid number: " + parts[i]);
//                             }
//                         }
//                     } else {
//                         try {
//                             double val = Double.parseDouble(valueStr);
//                             Metric metric = new Metric();
//                             metric.setName((String) data.get("name"));
//                             metric.setTask((String) data.get("producedByTask"));
//                             metric.setValue(val);
//                             metric.setTimestamp(getTimestampFromWorkflowData(data));
//                             metrics.add(metric);
//                         } catch (NumberFormatException e) {
//                             System.err.println("Invalid single value: " + valueStr);
//                             return ResponseEntity.badRequest().build();
//                         }
//                     }
//                 } else {
//                     return ResponseEntity.badRequest().build();
//                 }
//             }

//             return ResponseEntity.ok(metrics);

//         } catch (Exception e) {
//             e.printStackTrace();
//             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//         }
//     }

//     private long getTimestampFromWorkflowData(Map<String, Object> workflowData) {
//         Object dateObj = workflowData.get("date");
//         return dateObj != null ? parseIsoDateToMillis((String) dateObj) : 0L;
//     }

//     @Override
//     public ResponseEntity<UserEvaluation> submitUserEvaluation(String experimentId, String runId,
//             UserEvaluation userEvaluation) {
//         return ResponseEntity.ok(userEvaluation);
//     }
// }



package gr.imsi.athenarc.xtremexpvisapi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.DataAsset;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Experiment;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Run;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Run.Status;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Task;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.UserEvaluation;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Metric;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.MetricDefinition;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Param;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    // private void extractDatasets(Map<String, Object> workflowData, String
    // datasetKey, DataAsset.Role role,
    // List<DataAsset> dataAssets) {
    // List<Map<String, Object>> datasets = (List<Map<String, Object>>)
    // workflowData.get(datasetKey);
    // if (datasets != null) {
    // for (Map<String, Object> dataset : datasets) {
    // DataAsset dataAsset = new DataAsset();
    // dataAsset.setName((String) dataset.get("name"));
    // dataAsset.setSource((String) dataset.get("uri"));
    // dataAsset.setRole(role);

    // dataAssets.add(dataAsset);
    // }
    // }
    // }
    private void extractDatasets(Map<String, Object> taskObj, String key, DataAsset.Role role, String taskName,
            List<DataAsset> dataAssets) {
        if (taskObj.containsKey(key)) {
            List<Map<String, Object>> datasetList = (List<Map<String, Object>>) taskObj.get(key);
            for (Map<String, Object> dataset : datasetList) {
                String name = (String) dataset.get("name");
                String uri = (String) dataset.get("uri");
                Map<String, String> tags = new HashMap<>();

                if (dataset.containsKey("metadata")) {
                    Map<String, Object> metadata = (Map<String, Object>) dataset.get("metadata");
                    metadata.forEach((metaKey, metaValue) -> tags.put(metaKey, metaValue.toString()));
                }

                dataAssets.add(new DataAsset(name, "unknown", uri, "unknown", role, taskName, tags));
            }
        }
    }

    public ExtremeXPExperimentService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public ResponseEntity<List<Experiment>> getExperiments(int limit, int offset) {
        String requestUrl = workflowsApiUrl + "/experiments"; // API URL
        HttpHeaders headers = new HttpHeaders();
        headers.set("access-token", workflowsApiKey); // API Key
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

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
                    HttpEntity<Map<String, Object>> entityForMetrics = new HttpEntity<>(requestBody, headers);

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
                                metricDefinition.setDescription("Description not available");
                                metricDefinition.setUnit("unit not available");
                                metricDefinition.setGreaterIsBetter(null); // Set based on your logic

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
    public ResponseEntity<Experiment> getExperimentById(String experimentId) {
        String requestUrl = workflowsApiUrl + "/experiments/" + experimentId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("access-token", workflowsApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

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
                HttpEntity<Map<String, Object>> entityForMetrics = new HttpEntity<>(requestBody, headers);

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
                            metricDefinition.setDescription("Description not available");
                            metricDefinition.setUnit("unit not available");
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

    @Override
    public ResponseEntity<List<Run>> getRunsForExperiment(String experimentId) {
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
                Map<String, Object> experimentData = (Map<String, Object>) response.getBody().get("experiment");
                // System.out.println("experimentData: " + experimentData.get("workflow_ids"));

                List<String> workflowIds = (List<String>) experimentData.get("workflow_ids");
                List<Run> runs = new ArrayList<>();

                for (String runId : workflowIds.subList(0, 15)) {
                    ResponseEntity<Run> runResponse = getRunById(experimentId, runId);
                    if (runResponse.getStatusCode() == HttpStatus.OK && runResponse.getBody() != null) {
                        runs.add(runResponse.getBody());
                    }
                }

                return ResponseEntity.ok(runs);
            } else {
                return ResponseEntity.status(response.getStatusCode()).build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public ResponseEntity<Run> getRunById(String experimentId, String runId) {
        // Step 1: Get experiment details
        HttpHeaders headers = new HttpHeaders();
        headers.set("access-token", workflowsApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String workflowUrl = workflowsApiUrl + "/workflows/" + runId;
        ResponseEntity<Map> workflowResponse = restTemplate.exchange(
                workflowUrl, HttpMethod.GET, entity, Map.class);
        Map<String, Object> workflowData = (Map<String, Object>) workflowResponse.getBody().get("workflow");
        // Step 4: Convert response to Run object
        Run run = new Run();
        run.setId(runId);
        run.setName((String) workflowData.get("name"));
        run.setExperimentId((String) experimentId);
        run.setStartTime(parseIsoDateToMillis((String) workflowData.get("start")));
        run.setEndTime(parseIsoDateToMillis((String) workflowData.get("end")));
        String statusStr = (String) workflowData.get("status"); // Get status as string
        try {
            run.setStatus(Status.valueOf(statusStr.toUpperCase())); // Convert to Enum
        } catch (IllegalArgumentException e) {
            run.setStatus(Status.FAILED); // Default or handle unknown status
        }
        // Extract and store metadata in tags
        Map<String, String> tags = new HashMap<>();
        if (workflowData.containsKey("metadata")) {
            Map<String, Object> metadata = (Map<String, Object>) workflowData.get("metadata");
            for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                if (entry.getValue() instanceof String) {
                    tags.put(entry.getKey(), (String) entry.getValue()); // Add metadata to tags
                } else {
                    tags.put(entry.getKey(), entry.getValue().toString()); // Convert non-string values to String
                }
            }
        }

        // Set only metadata tags to the Run object
        run.setTags(tags);

        List<String> metricIds = new ArrayList<>();
        Object workflowIdsObj = workflowData.get("metric_ids");
        if (workflowIdsObj instanceof List<?>) {
            metricIds = ((List<?>) workflowIdsObj).stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .collect(Collectors.toList());
        }

        // List<Metric> metrics = new ArrayList<>();
        // for (String metricId : metricIds) {
        // ResponseEntity<List<Metric>> metricResponse = getMetricValues(experimentId,
        // runId, metricId);
        // if (metricResponse.getBody() != null) {
        // metrics.addAll(metricResponse.getBody()); // Append all extracted metrics
        // }
        // }
        // run.setMetrics(metrics);

        List<Metric> finalMetrics = new ArrayList<>();

        for (String metricId : metricIds) {
            ResponseEntity<List<Metric>> metricResponse = getMetricValues(experimentId, runId, metricId);
            List<Metric> fetchedMetrics = metricResponse.getBody();

            if (fetchedMetrics == null || fetchedMetrics.isEmpty()) {
                continue;
            }

            // Case 1: Single-value metric (step is null)
            if (fetchedMetrics.size() == 1 && fetchedMetrics.get(0).getStep() == null) {
                finalMetrics.add(fetchedMetrics.get(0));
            }
            // Case 2: Multi-step metric - pick the one with the highest step
            else {
                Metric highestStepMetric = fetchedMetrics.stream()
                        .filter(m -> m.getStep() != null)
                        .max(Comparator.comparingInt(Metric::getStep))
                        .orElse(null);

                if (highestStepMetric != null) {
                    finalMetrics.add(highestStepMetric);
                }
            }
        }

        run.setMetrics(finalMetrics);

        List<Task> tasks = new ArrayList<>();
        List<Param> params = new ArrayList<>();
        List<DataAsset> dataAssets = new ArrayList<>();

        Object tasksObj = workflowData.get("tasks");
        if (tasksObj instanceof List<?>) {
            List<Map<String, Object>> tasksList = (List<Map<String, Object>>) tasksObj;

            for (Map<String, Object> taskObj : tasksList) {
                // Extract task details
                String taskName = (String) taskObj.get("name");
                String taskType = (String) taskObj.get("source_code");
                String taskId = (String) taskObj.get("id");
                Long taskStartTime = parseIsoDateToMillis((String) taskObj.get("start"));
                Long taskEndTime = parseIsoDateToMillis((String) taskObj.get("end"));

                Map<String, String> taskTags = new HashMap<>();
                String variant = null;
                Object metadataObj = taskObj.get("metadata");
                if (metadataObj instanceof Map<?, ?>) {
                    Map<String, Object> metadataMap = (Map<String, Object>) metadataObj;
                    variant = (String) metadataMap.get("prototypical_name");
                }
                // Extract parameters for this task
                if (taskObj.containsKey("parameters")) {
                    List<Map<String, Object>> parameters = (List<Map<String, Object>>) taskObj.get("parameters");
                    for (Map<String, Object> paramObj : parameters) {
                        String paramName = (String) paramObj.get("name");
                        String paramValue = (String) paramObj.get("value");
                        if (paramName != null && paramValue != null) {
                            params.add(new Param(paramName, paramValue, taskId)); // Assign task to Param
                        }
                    }
                }

                // Create task object and add it to the list
                Task task = new Task(taskName, taskType, variant, taskStartTime, taskEndTime, taskTags, taskId);
                tasks.add(task);

                // Extract datasets for the task
                extractDatasets(taskObj, "input_datasets", DataAsset.Role.INPUT, taskId, dataAssets);
                extractDatasets(taskObj, "output_datasets", DataAsset.Role.OUTPUT, taskId, dataAssets);
            }
        }

        // Set extracted details into the Run object
        run.setParams(params); // Now Params have task association
        run.setTasks(tasks);
        run.setDataAssets(dataAssets);

        return ResponseEntity.ok(run);

    }

    @Override
    public ResponseEntity<List<Metric>> getMetricValues(String experimentId, String runId, String metricName) {
        String requestUrl = workflowsApiUrl + "/metrics/" + metricName; // API URL
        HttpHeaders headers = new HttpHeaders();
        headers.set("access-token", workflowsApiKey); // API Key
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> workflowResponse = restTemplate.exchange(
                requestUrl, HttpMethod.GET, entity, Map.class);
        System.out.println("workflowResponse: " + workflowResponse);
        try {
            Map<String, Object> workflowData = workflowResponse.getBody();
            if (workflowData == null) {
                return ResponseEntity.badRequest().build();
            }

            // list to collect metrics
            List<Metric> metrics = new ArrayList<>();
            Object rawValue = workflowData.get("value");

            if (rawValue instanceof Number) {
                Metric metric = new Metric();
                metric.setName((String) workflowData.get("name"));
                metric.setTask((String) workflowData.get("producedByTask"));
                metric.setValue(((Number) rawValue).doubleValue());
                metric.setTimestamp(getTimestampFromWorkflowData(workflowData));
                metrics.add(metric);
            } else if (rawValue instanceof String) {
                String valueStr = ((String) rawValue).trim();
                if (valueStr.startsWith("[") && valueStr.endsWith("]")) {
                    valueStr = valueStr.substring(1, valueStr.length() - 1);
                    String[] parts = valueStr.split(",");
                    for (int i = 0; i < parts.length; i++) {
                        try {
                            double val = Double.parseDouble(parts[i].trim());
                            Metric metric = new Metric();
                            metric.setName((String) workflowData.get("name"));
                            metric.setTask((String) workflowData.get("producedByTask"));
                            metric.setValue(val);
                            metric.setStep(i);
                            metric.setTimestamp(getTimestampFromWorkflowData(workflowData));
                            metrics.add(metric);
                        } catch (NumberFormatException e) {
                            System.err.println("Skipping invalid number: " + parts[i]);
                        }
                    }
                } else {
                    try {
                        double val = Double.parseDouble(valueStr);
                        Metric metric = new Metric();
                        metric.setName((String) workflowData.get("name"));
                        metric.setTask((String) workflowData.get("producedByTask"));
                        metric.setValue(val);
                        metric.setTimestamp(getTimestampFromWorkflowData(workflowData));
                        metrics.add(metric);
                    } catch (NumberFormatException e) {
                        return ResponseEntity.badRequest().build();
                    }
                }
            } else {
                return ResponseEntity.badRequest().build();
            }

            return ResponseEntity.ok(metrics);

        } catch (Exception e) {
            e.printStackTrace(); // optional
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // fallback for unexpected issues
        }
    }

    private long getTimestampFromWorkflowData(Map<String, Object> workflowData) {
        Object dateObj = workflowData.get("date");
        return dateObj != null ? parseIsoDateToMillis((String) dateObj) : 0L;
    }

    @Override
    public ResponseEntity<UserEvaluation> submitUserEvaluation(String experimentId, String runId,
            UserEvaluation userEvaluation) {
        return ResponseEntity.ok(userEvaluation);
    }
}

