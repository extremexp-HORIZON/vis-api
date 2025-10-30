package gr.imsi.athenarc.xtremexpvisapi.service.explainability;

import gr.imsi.athenarc.xtremexpvisapi.controller.DataController;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Param;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Run;
import gr.imsi.athenarc.xtremexpvisapi.service.experiment.ExperimentService;
import gr.imsi.athenarc.xtremexpvisapi.service.experiment.ExperimentServiceFactory;
import gr.imsi.athenarc.xtremexpvisapi.service.shared.MlAnalysisResourceHelper;
import lombok.extern.java.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;

import explainabilityService.DataPaths;
import explainabilityService.ExplanationsRequest;
import explainabilityService.FeatureImportanceRequest;
import explainabilityService.HyperparameterList;
import explainabilityService.Hyperparameters;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generic helper for explainability-related run selection.
 * <p>
 * This class provides logic to identify other runs within the same experiment
 * that are
 * considered "similar" to a reference run.
 * <p>
 * <strong>Current heuristic:</strong> runs are considered similar if they
 * define the exact
 * same set of parameter keys, regardless of the values used.
 */
@Component
@Log
public class ExplainabilityRunHelper {
    private static final Logger LOG = LoggerFactory.getLogger(DataController.class);

    private final ExperimentServiceFactory experimentServiceFactory;
    private final MlAnalysisResourceHelper mlAnalysisResourceHelper;

    public ExplainabilityRunHelper(ExperimentServiceFactory experimentServiceFactory,
            MlAnalysisResourceHelper mlAnalysisResourceHelper) {
        this.mlAnalysisResourceHelper = mlAnalysisResourceHelper;
        this.experimentServiceFactory = experimentServiceFactory;
    }

    /**
     * Finds other completed runs in the same experiment that are considered
     * "similar"
     * to the given reference run for explainability purposes.
     * <p>
     * Current logic considers runs similar if they define the same parameter keys.
     *
     * @param referenceRun the run used as a baseline for comparison
     * @return list of other completed runs with matching parameter structure
     */
    private List<Run> findSimilarRuns(Run referenceRun) {
        Set<String> referenceKeys = getParamNames(referenceRun);

        if (referenceKeys.isEmpty()) {
            log.info("No parameters found for reference run: " + referenceRun.getId());
            return Collections.emptyList();
        }

        ExperimentService service = experimentServiceFactory.getActiveService();
        ResponseEntity<List<Run>> response = service.getRunsForExperiment(referenceRun.getExperimentId());
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return Collections.emptyList();
        }

        return response.getBody().stream()
                .filter(run -> !run.getId().equals(referenceRun.getId()))
                .filter(run -> run.getStatus() == Run.Status.COMPLETED)
                .filter(run -> getParamNames(run).equals(referenceKeys))
                .collect(Collectors.toList());
    }

    private Set<String> getParamNames(Run run) {
        if (run.getParams() == null) {
            log.info("Parameters list is null");
            return Collections.emptySet();
        }

        Set<String> paramNames = run.getParams().stream()
                .map(Param::getName)
                .filter(Objects::nonNull) // Filter out any null names
                .collect(Collectors.toSet());
        log.info("Parameter names: " + paramNames);

        return paramNames;
    }

    /**
     * Gets metric value by name, or uses index 0 as fallback
     */
    private double getMetricValue(Run run, String targetMetricName) {
        if (run.getMetrics() == null || run.getMetrics().isEmpty()) {
            throw new IllegalArgumentException("No metrics found for run: " + run.getId());
        }
        
        if (targetMetricName == null || targetMetricName.trim().isEmpty()) {
            return run.getMetrics().get(0).getValue();
        }
        
        return run.getMetrics().stream()
                .filter(metric -> metric.getName().equalsIgnoreCase(targetMetricName.trim()))
                .findFirst()
                .map(metric -> metric.getValue())
                .orElse(run.getMetrics().get(0).getValue());
    }

    /**
     * Extracts target_metric from JSON and removes it so protobuf parsing works
     */
    private String[] extractAndCleanTargetMetric(String explainabilityRequest) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(explainabilityRequest);
            String targetMetric = jsonNode.has("target_metric") ? jsonNode.get("target_metric").asText() : null;
            
            // Remove target_metric field
            if (jsonNode.has("target_metric")) {
                com.fasterxml.jackson.databind.node.ObjectNode objectNode = 
                    (com.fasterxml.jackson.databind.node.ObjectNode) jsonNode;
                objectNode.remove("target_metric");
                String cleanedRequest = mapper.writeValueAsString(objectNode);
                return new String[]{targetMetric, cleanedRequest};
            }
            
            return new String[]{targetMetric, explainabilityRequest};
        } catch (Exception e) {
            LOG.warn("Could not process target_metric: " + e.getMessage());
            return new String[]{null, explainabilityRequest};
        }
    }

    public FeatureImportanceRequest featureImportanceRequestBuilder(String featureImportanceRequest,
            String experimentId, String runId,
            String authorization) {
        // Fetch metadata, paths, etc. like you do in requestBuilder
        // Pseudo-code example:
        ObjectMapper mapper = new ObjectMapper();
        String type;
        try {
            JsonNode jsonNode = mapper.readTree(featureImportanceRequest);
            type = jsonNode.has("type") ? jsonNode.get("type").asText() : "FeatureImportance";
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid featureImportanceRequest JSON", e);
        }

        // Create a DataPaths object from the loaded data paths
        Optional<Map<String, String>> dataPaths = loadExplainabilityDataPaths(experimentId, runId, authorization,
                "feature");
        if (dataPaths.isEmpty()) {
            throw new IllegalArgumentException("No data paths found for experimentId this experiment");
        }
        DataPaths data = buildDataPaths(dataPaths.get());

        // Get model path using the helper method
        String modelPath = findModelPath(dataPaths.get());

        // System.out.println("Model path: " + modelPath);
        // System.out.println("data {}"+data);

        // Add model to request
        return FeatureImportanceRequest.newBuilder()
                .addModel(modelPath)
                .setData(data)
                .setType(type)
                .build();
    }

    protected ExplanationsRequest requestBuilder(String explainabilityRequest, String experimentId, String runId,
            String authorization)
            throws JsonProcessingException, InvalidProtocolBufferException {

        // Extract target_metric and clean the JSON for protobuf parsing
        String[] result = extractAndCleanTargetMetric(explainabilityRequest);
        String targetMetricName = result[0];
        String cleanedRequest = result[1];

        // Parse the cleaned JSON request into a Protobuf object
        ExplanationsRequest.Builder requestBuilder = ExplanationsRequest.newBuilder();
        JsonFormat.parser().merge(cleanedRequest, requestBuilder);

        if (requestBuilder.getExplanationType().equals("featureExplanation")) {

            // Create a DataPaths object from the loaded data paths
            Optional<Map<String, String>> dataPaths = loadExplainabilityDataPaths(experimentId, runId, authorization,
                    "feature");
            if (dataPaths.isEmpty()) {
                throw new IllegalArgumentException("No data paths found for experimentId this experiment");
            }
            DataPaths data = buildDataPaths(dataPaths.get());
            requestBuilder.setData(data);

            // Get model path using the helper method
            String modelPath = findModelPath(dataPaths.get());

            // Add model to request
            requestBuilder.addAllModel(List.of(modelPath));
            // System.out.println("requstbuildr "+requestBuilder);

        } else if (requestBuilder.getExplanationType().equals("hyperparameterExplanation")) {
            ExperimentService service = experimentServiceFactory.getActiveService();
            ResponseEntity<Run> response = service.getRunById(experimentId, runId);
            Run run = response.getBody();
            List<Run> similarRuns = findSimilarRuns(run);
            similarRuns.add(run);

            Optional<Map<String, String>> dataPaths = loadExplainabilityDataPaths(experimentId, runId, authorization,
                    "hyperparameter");
            if (requestBuilder.getExplanationMethod().equals("ale") && requestBuilder.getFeature1().isEmpty()) {
                requestBuilder.setFeature1(findFirstDifferingParameter(similarRuns)
                        .orElseThrow(() -> new IllegalArgumentException("No differing parameters found")));
            }
            // Get model path using the helper method and add to request
            String modelPath = findModelPath(dataPaths.get());
            requestBuilder.addAllModel(List.of(modelPath));

            for (Run similarRun : similarRuns) {
                dataPaths = loadExplainabilityDataPaths(similarRun.getExperimentId(),
                        similarRun.getId(), authorization, "hyperparameter");
                Hyperparameters.Builder hyperparametersBuilder = Hyperparameters.newBuilder();
                double metricValue = getMetricValue(similarRun, targetMetricName);
                hyperparametersBuilder.setMetricValue((float) metricValue);
                List<Param> params = similarRun.getParams();
                for (Param param : params) {
                    // Create a new builder for each parameter
                    HyperparameterList.Builder hyperparameterListBuilder = HyperparameterList.newBuilder();
                    hyperparameterListBuilder.setValues(param.getValue());
                    try {
                        Double.parseDouble(param.getValue());
                        hyperparameterListBuilder.setType("numeric");
                    } catch (NumberFormatException e) {
                        hyperparameterListBuilder.setType("categorical");
                    }
                    HyperparameterList hyperparameterList = hyperparameterListBuilder.build();
                    hyperparametersBuilder.putHyperparameter(param.getName(), hyperparameterList);
                }

                Hyperparameters hyperparameters = hyperparametersBuilder.build();
                // Get model path using the helper method and add to hyper configs
                String hyperModelPath = findModelPath(dataPaths.get());
                requestBuilder.putHyperConfigs(hyperModelPath, hyperparameters);
            }
            LOG.info("Similar runs: " + similarRuns.size());
        
        } else if (requestBuilder.getExplanationType().equals("experimentExplanation")) {
            ExperimentService service = experimentServiceFactory.getActiveService();
            
            ResponseEntity<List<Run>> resp = service.getRunsForExperiment(experimentId);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw new IllegalArgumentException("Could not fetch runs for experimentId: " + experimentId);
            }
            List<Run> runs = resp.getBody().stream()
                .filter(run -> run.getStatus() == Run.Status.COMPLETED)
                .collect(Collectors.toList());

            // // find variability points / hyperparams common to all runs
            // Set<String> commonParamKeys = runs.stream()
            //     .map(this::getParamNames)
            //     .filter(keys -> !keys.isEmpty())
            //     .reduce((set1, set2) -> {
            //         set1.retainAll(set2);
            //         return set1;
            //     })
            //     .orElseThrow(() -> new IllegalArgumentException("No runs with parameters found in experiment: " + experimentId));
            // LOG.info("Common parameter keys across all runs: " + commonParamKeys);

            // if method is ALE and no feature specified, find first differing param
            // if no differing param found, throw error
            if (requestBuilder.getExplanationMethod().equals("ale") && requestBuilder.getFeature1().isEmpty()) {
                requestBuilder.setFeature1(
                    findFirstDifferingParameter(runs)
                        .orElseThrow(() -> new IllegalArgumentException("No differing parameters found"))
                );
            }

            // build request that includes all common hyperparams

            List<String> runs_target_metric_names = new ArrayList<>();
            List<Double> runs_target_metric_values = new ArrayList<>();
            for (Run run : runs) {
                // Set<String> runParamKeys = getParamNames(run);
                // if (!runParamKeys.containsAll(commonParamKeys)) {
                //     LOG.warn("Run " + run.getId() + " does not contain all common parameters, skipping.");
                //     continue;
                // }

                Optional<Map<String, String>> dataPaths = loadExplainabilityDataPaths(
                    run.getExperimentId(),
                    run.getId(),
                    authorization,
                    "hyperparameter"
                );
                if (dataPaths.isEmpty()) {
                    LOG.warn("No data paths found for run: " + run.getId() + ", skipping.");
                    continue;
                }

                Hyperparameters.Builder hyperparametersBuilder = Hyperparameters.newBuilder();
                double metricValue = getMetricValue(run, targetMetricName);
                hyperparametersBuilder.setMetricValue((float) metricValue);
                
                // Get the metric name that was actually used
                String actualMetricName = (targetMetricName != null && !targetMetricName.trim().isEmpty()) ? 
                    run.getMetrics().stream()
                        .filter(metric -> metric.getName().equalsIgnoreCase(targetMetricName.trim()))
                        .findFirst()
                        .map(metric -> metric.getName())
                        .orElse(run.getMetrics().get(0).getName()) :
                    run.getMetrics().get(0).getName();
                    
                runs_target_metric_names.add(actualMetricName);
                runs_target_metric_values.add(metricValue);

                List<Param> params = run.getParams();
                for (Param param : params) {
                    // String paramName = param.getName();
                    // if (!commonParamKeys.contains(paramName)) {
                    //     continue; // skip params not in common keys
                    // }

                    // Create a new builder for each parameter
                    HyperparameterList.Builder hyperparameterListBuilder = HyperparameterList.newBuilder();
                    hyperparameterListBuilder.setValues(param.getValue());
                    try {
                        Double.parseDouble(param.getValue());
                        hyperparameterListBuilder.setType("numeric");
                    } catch (NumberFormatException e) {
                        hyperparameterListBuilder.setType("categorical");
                    }
                    HyperparameterList hyperparameterList = hyperparameterListBuilder.build();
                    hyperparametersBuilder.putHyperparameter(param.getName(), hyperparameterList);
                }

                Hyperparameters hyperparameters = hyperparametersBuilder.build();
                // Get model path using the helper method and add to hyper configs
                String hyperModelPath = findModelPath(dataPaths.get());
                requestBuilder.putExperimentConfigs(hyperModelPath, hyperparameters);
            }
            LOG.info("Number of runs: " + runs.size());
            LOG.info("Runs target metric names: " + runs_target_metric_names);
            LOG.info("Runs target metric values: " + runs_target_metric_values);

        } else {
            throw new IllegalArgumentException("Invalid explanation type: " + requestBuilder.getExplanationType());
        }
        return requestBuilder.build();
    }

    /**
     * Finds the first parameter whose value differs across the given runs.
     * 
     * @param runs the list of runs to check for differences in parameter values
     * @return the name of the first differing parameter, or empty if all parameters
     *         have identical values
     */
    private Optional<String> findFirstDifferingParameter(List<Run> runs) {
        if (runs == null || runs.size() <= 1) {
            throw new IllegalArgumentException("Run List is empty");
        }

        // Get the list of param names from the first run
        List<Param> firstRunParams = runs.get(0).getParams();
        if (firstRunParams == null || firstRunParams.isEmpty()) {
            throw new IllegalArgumentException("First run has no parameters");
        }

        // Check each parameter across all runs
        for (Param param : firstRunParams) {
            String paramName = param.getName();
            String firstValue = param.getValue();

            // Check if this parameter has different values across runs
            boolean hasDifferentValues = runs.stream()
                    .skip(1) // Skip the first run we already got the value from
                    .map(run -> run.getParams().stream()
                            .filter(p -> p.getName().equals(paramName))
                            .findFirst()
                            .map(Param::getValue)
                            .orElse(null))
                    .anyMatch(value -> value == null || !value.equals(firstValue));

            if (hasDifferentValues) {
                return Optional.of(paramName);
            }
        }
        // If no differing parameters were found, return empty
        return Optional.empty();
    }

    @Cacheable(value = "explainabilityDataPaths", key = "#experimentId + '::' + #runId")
    public Optional<Map<String, String>> loadExplainabilityDataPaths(String experimentId, String runId,
            String authorization, String explanationType) {
        log.info("Loading evaluation data for experimentId: " + experimentId + ", runId: " + runId);
        ExperimentService service = experimentServiceFactory.getActiveService();
        ResponseEntity<Run> response = service.getRunById(experimentId, runId);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return Optional.empty();
        }

        Run run = response.getBody();
        Optional<Map<String, String>> dataPaths = mlAnalysisResourceHelper.getRequiredFilePaths(run, authorization,
                explanationType);
        if (dataPaths.isEmpty()) {
            log.warning("No data paths found for experimentId: " + experimentId + ", runId: " + runId);
            return Optional.empty();
        }

        return dataPaths;
    }

    private static final Map<String, List<String>> ALIASES = new HashMap<>();
    private static final List<String> MODEL_FILES = Arrays.asList("model.pkl", "model.pt", "model");
    static {
        ALIASES.put("xTest", Arrays.asList("X_test.csv", "x_test"));
        ALIASES.put("xTrain", Arrays.asList("X_train.csv", "x_train"));
        ALIASES.put("yTest", Arrays.asList("Y_test.csv", "y_test"));
        ALIASES.put("yTrain", Arrays.asList("Y_train.csv", "y_train"));
        ALIASES.put("yPred", Arrays.asList("Y_pred.csv", "y_pred"));
    }

    /**
     * Finds the first available model file from the data paths.
     * 
     * @param dataPaths Map of file names to their paths
     * @return The path to the first found model file
     * @throws IllegalArgumentException if no model file is found
     */
    private String findModelPath(Map<String, String> dataPaths) {
        return MODEL_FILES.stream()
                .map(dataPaths::get)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Missing model path: expected one of " + MODEL_FILES));
    }

    private String resolve(Map<String, String> map, String logicalKey) {
        return ALIASES.get(logicalKey).stream()
                .map(map::get)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Missing key for " + logicalKey +
                        " (accepted: " + ALIASES.get(logicalKey) + ")"));
    }

    private DataPaths buildDataPaths(Map<String, String> dataPaths) {
        DataPaths.Builder b = DataPaths.newBuilder()
                .setXTest(resolve(dataPaths, "xTest"))
                .setXTrain(resolve(dataPaths, "xTrain"))
                .setYTest(resolve(dataPaths, "yTest"))
                .setYTrain(resolve(dataPaths, "yTrain"))
                .setYPred(resolve(dataPaths, "yPred"));

        return b.build();
    }

}
