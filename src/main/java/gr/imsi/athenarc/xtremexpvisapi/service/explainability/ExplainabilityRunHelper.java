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


    public FeatureImportanceRequest featureImportanceRequestBuilder(String experimentId, String runId, String authorization) {
    // Fetch metadata, paths, etc. like you do in requestBuilder
    // Pseudo-code example:

   // Create a DataPaths object from the loaded data paths
            Optional<Map<String, String>> dataPaths = loadExplainabilityDataPaths(experimentId, runId, authorization,
                    "feature");
            if (dataPaths.isEmpty()) {
                throw new IllegalArgumentException("No data paths found for experimentId this experiment");
            }
            DataPaths data = buildDataPaths(dataPaths.get());

            // Get model path, try "model.pkl" first, then "model"
            String modelPath = dataPaths.get().get("model.pkl");
            if (modelPath == null) {
                modelPath = dataPaths.get().get("model");
            }

            if (modelPath == null) {
                throw new IllegalArgumentException("Missing model path: expected either 'model.pkl' or 'model'");
            }

            System.out.println("Model path: " + modelPath);
            System.out.println("data {}"+data);

            // Add model to request
            List<String> model = List.of(modelPath.toString());
    return FeatureImportanceRequest.newBuilder()
        .addModel(modelPath)
        .setData(data)
        .build();
}

    protected ExplanationsRequest requestBuilder(String explainabilityRequest, String experimentId, String runId,
            String authorization)
            throws JsonProcessingException, InvalidProtocolBufferException {

        // Parse the JSON request into a Protobuf object
        ExplanationsRequest.Builder requestBuilder = ExplanationsRequest.newBuilder();
        JsonFormat.parser().merge(explainabilityRequest, requestBuilder);

        if (requestBuilder.getExplanationType().equals("featureExplanation")) {

            // Create a DataPaths object from the loaded data paths
            Optional<Map<String, String>> dataPaths = loadExplainabilityDataPaths(experimentId, runId, authorization,
                    "feature");
            if (dataPaths.isEmpty()) {
                throw new IllegalArgumentException("No data paths found for experimentId this experiment");
            }
            DataPaths data = buildDataPaths(dataPaths.get());
            requestBuilder.setData(data);

            // Get model path, try "model.pkl" first, then "model"
            String modelPath = dataPaths.get().get("model.pkl");
            if (modelPath == null) {
                modelPath = dataPaths.get().get("model");
            }

            if (modelPath == null) {
                throw new IllegalArgumentException("Missing model path: expected either 'model.pkl' or 'model'");
            }

            // Add model to request
            List<String> model = List.of(modelPath.toString());
            requestBuilder.addAllModel(model);
            System.out.println("requstbuildr "+requestBuilder);

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
            List<String> model = new ArrayList<>();
            if (service.getClass().getSimpleName().equals("MLflowExperimentService")) {
                model.add(dataPaths.get().get("model.pkl").toString());
                requestBuilder.addAllModel(model);
            } else {
                model.add(dataPaths.get().get("model").toString());
                requestBuilder.addAllModel(model);

            }

            for (Run similarRun : similarRuns) {
                dataPaths = loadExplainabilityDataPaths(similarRun.getExperimentId(),
                        similarRun.getId(), authorization, "hyperparameter");
                Hyperparameters.Builder hyperparametersBuilder = Hyperparameters.newBuilder();
                hyperparametersBuilder.setMetricValue((float) run.getMetrics().get(0).getValue());

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
                if(service.getClass().getSimpleName().equals("MLflowExperimentService")) {
                    requestBuilder.putHyperConfigs(dataPaths.get().get("model.pkl").toString(), hyperparameters);
                } else {
                    requestBuilder.putHyperConfigs(dataPaths.get().get("model").toString(), hyperparameters);
                }
            }
            LOG.info("Similar runs: " + similarRuns.size());

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
    static {
        ALIASES.put("xTest", Arrays.asList("X_test.csv", "x_test"));
        ALIASES.put("xTrain", Arrays.asList("X_train.csv", "x_train"));
        ALIASES.put("yTest", Arrays.asList("Y_test.csv", "y_test"));
        ALIASES.put("yTrain", Arrays.asList("Y_train.csv", "y_train"));
        ALIASES.put("yPred", Arrays.asList("Y_pred.csv", "y_pred"));
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
