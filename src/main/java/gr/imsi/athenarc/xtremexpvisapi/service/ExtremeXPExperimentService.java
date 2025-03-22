package gr.imsi.athenarc.xtremexpvisapi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.DataAsset;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Experiment;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Run;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Run.Status;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Task;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Metric;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Param;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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
                        .toList();

                // Fetch missing timestamps in parallel

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
            ResponseEntity<Map> response = restTemplate.exchange(
                    requestUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> experimentData = (Map<String, Object>) response.getBody().get("experiment");

                Experiment experiment = mapToExperiment(experimentData);
                return ResponseEntity.ok(experiment);
            } else {
                return ResponseEntity.status(response.getStatusCode()).build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Experiment mapToExperiment(Map<String, Object> data) {
        Experiment experiment = new Experiment();
        experiment.setId((String) data.get("id"));
        experiment.setName((String) data.get("name"));
        Map<String, String> tags = new HashMap<>();
        tags.put("status", (String) data.get("status"));
        Object workflowIdsObj = data.get("workflow_ids");
        if (workflowIdsObj instanceof List<?>) {
            List<?> rawList = (List<?>) workflowIdsObj;
            List<String> workflowIds = rawList.stream()
                    .filter(String.class::isInstance) // Ensure only Strings
                    .map(String.class::cast)
                    .collect(Collectors.toList());
            tags.put("workflow_ids", String.join(",", workflowIds)); // Store as CSV string
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

    @Override
    public ResponseEntity<List<Run>> getRunsForExperiment(String experimentId) {
        // make it empty
        return null;
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
        Map<String, String> tags = new HashMap<>();
        Object workflowIdsObj = workflowData.get("metric_ids");
        if (workflowIdsObj instanceof List<?>) {
            List<?> rawList = (List<?>) workflowIdsObj;
            List<String> workflowIds = rawList.stream()
                    .filter(String.class::isInstance) // Ensure only Strings
                    .map(String.class::cast)
                    .collect(Collectors.toList());
            tags.put("metric_ids", String.join(",", workflowIds)); // Store as CSV string
        }
        run.setTags(tags);
        List pame = new ArrayList();
        // i want to use for each metrics id the getMetricValues and set my
        // run.setMetrics to this
        for (String metricId : tags.get("metric_ids").split(",")) {
            Metric metric = getMetricValues(experimentId, runId, metricId).getBody();
            System.out.println("metricId: " + metric);
            pame.add(metric);
        }
        run.setMetrics(pame);

        List<Param> params = new ArrayList<>();
        // workflow parmeats
        Object workflowParamsObj = workflowData.get("parameters");
        if (workflowParamsObj instanceof List<?>) {
            List<Map<String, Object>> workflowParams = (List<Map<String, Object>>) workflowParamsObj;
            for (Map<String, Object> paramObj : workflowParams) {
                String paramName = (String) paramObj.get("name");
                String paramValue = (String) paramObj.get("value");

                if (paramName != null && paramValue != null) {
                    params.add(new Param(paramName, paramValue));
                }
            }
        }
        run.setParams(params);

        // List<Task> task = new ArrayList<>();
        // Object tasksObj = workflowData.get("tasks");
        // if (tasksObj instanceof List<?>) {
        // List<Map<String, Object>> tasks = (List<Map<String, Object>>) tasksObj;
        // for (Map<String, Object> taskObj : tasks) {
        // String taskName = (String) taskObj.get("name");
        // String taskType = (String) taskObj.get("source_code");
        // Long taskStartTime = parseIsoDateToMillis((String) taskObj.get("start"));
        // Long taskEndTime = parseIsoDateToMillis((String) taskObj.get("end"));

        // task.add(new Task(taskName, taskType, taskStartTime, taskEndTime, null));
        // }
        // }
        // run.setTasks(task);
        List<Task> tasks = new ArrayList<>();
        Object tasksObj = workflowData.get("tasks");

        if (tasksObj instanceof List<?>) {
            List<Map<String, Object>> taskList = (List<Map<String, Object>>) tasksObj;

            for (Map<String, Object> taskObj : taskList) {
                String taskName = (String) taskObj.get("name");
                String taskType = (String) taskObj.get("source_code");
                Long taskStartTime = parseIsoDateToMillis((String) taskObj.get("start"));
                Long taskEndTime = parseIsoDateToMillis((String) taskObj.get("end"));

                // Initialize task tags map
                Map<String, String> taskTags = new HashMap<>();

                // Extract task parameters
                if (taskObj.containsKey("parameters")) {
                    List<Map<String, Object>> parameters = (List<Map<String, Object>>) taskObj.get("parameters");
                    for (Map<String, Object> paramObj : parameters) {
                        String paramName = (String) paramObj.get("name");
                        String paramValue = (String) paramObj.get("value");
                        taskTags.put("param_" + paramName, paramValue); // Store as "param_paramName"
                    }
                }

                // Extract input datasets
                if (taskObj.containsKey("input_datasets")) {
                    List<Map<String, Object>> inputDatasets = (List<Map<String, Object>>) taskObj.get("input_datasets");
                    List<String> datasetNames = inputDatasets.stream()
                            .map(dataset -> (String) dataset.get("name"))
                            .collect(Collectors.toList());
                    taskTags.put("input_datasets", String.join(",", datasetNames));
                }

                // Extract output datasets
                if (taskObj.containsKey("output_datasets")) {
                    List<Map<String, Object>> outputDatasets = (List<Map<String, Object>>) taskObj
                            .get("output_datasets");
                    List<String> datasetNames = outputDatasets.stream()
                            .map(dataset -> (String) dataset.get("name"))
                            .collect(Collectors.toList());
                    taskTags.put("output_datasets", String.join(",", datasetNames));
                }

                // Create Task object and set tags
                Task task = new Task(taskName, taskType, taskStartTime, taskEndTime, null);
                task.setTags(taskTags); // Assuming Task class has setTags() method

                tasks.add(task);
            }
        }

        // Assign tasks to the run
        run.setTasks(tasks);

        List<DataAsset> dataAssets = new ArrayList<>();
        extractDatasets(workflowData, "input_datasets", DataAsset.Role.INPUT, dataAssets);
        extractDatasets(workflowData, "output_datasets", DataAsset.Role.OUTPUT, dataAssets);
        run.setDataAssets(dataAssets);

        return ResponseEntity.ok(run);

    }

    private void extractDataAssets(Map<String, Object> task, List<DataAsset> dataAssets) {
        // Extract input datasets
        extractDatasets(task, "input_datasets", DataAsset.Role.INPUT, dataAssets);

        // Extract output datasets
        extractDatasets(task, "output_datasets", DataAsset.Role.OUTPUT, dataAssets);
    }

    /**
     * Extracts datasets from a given key in the task and adds them to the
     * dataAssets list.
     */
    private void extractDatasets(Map<String, Object> workflowData, String datasetKey, DataAsset.Role role,
            List<DataAsset> dataAssets) {
        List<Map<String, Object>> datasets = (List<Map<String, Object>>) workflowData.get(datasetKey);
        if (datasets != null) {
            for (Map<String, Object> dataset : datasets) {
                DataAsset dataAsset = new DataAsset();
                dataAsset.setName((String) dataset.get("name"));
                dataAsset.setSource((String) dataset.get("uri"));
                dataAsset.setRole(role);

                dataAssets.add(dataAsset);
            }
        }
    }

    @Override
    public ResponseEntity<Metric> getMetricValues(String experimentId, String runId, String metricName) {
        String requestUrl = workflowsApiUrl + "/metrics/" + metricName; // API URL
        HttpHeaders headers = new HttpHeaders();
        headers.set("access-token", workflowsApiKey); // API Key
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> workflowResponse = restTemplate.exchange(
                requestUrl, HttpMethod.GET, entity, Map.class);
        Map<String, Object> workflowData = workflowResponse.getBody();
        // Step 4: Convert response to Run object
        Metric metric = new Metric();
        metric.setName((String) workflowData.get("name"));
        metric.setValue(new Double(workflowData.get("value").toString()));
        return ResponseEntity.ok(metric);
    }
}
