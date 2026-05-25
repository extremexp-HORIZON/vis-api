package gr.imsi.athenarc.xtremexpvisapi.service.mlevaluation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Run;
import gr.imsi.athenarc.xtremexpvisapi.domain.mlevaluation.ConfusionMatrixResult;
import gr.imsi.athenarc.xtremexpvisapi.domain.mlevaluation.ModelEvaluationSummary;
import gr.imsi.athenarc.xtremexpvisapi.domain.mlevaluation.ModelEvaluationSummary.ClassReportEntry;
import gr.imsi.athenarc.xtremexpvisapi.domain.mlevaluation.ModelEvaluationSummary.OverallMetrics;
import gr.imsi.athenarc.xtremexpvisapi.service.experiment.ExperimentService;
import gr.imsi.athenarc.xtremexpvisapi.service.experiment.ExperimentServiceFactory;
import gr.imsi.athenarc.xtremexpvisapi.service.explainability.ExplainabilityRunHelper;
import gr.imsi.athenarc.xtremexpvisapi.service.shared.MlAnalysisResourceHelper;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

@Service
public class ModelEvaluationService {

    private final ExperimentServiceFactory experimentServiceFactory;
    private final String mockEvaluationPathTemplate;
    private final MlAnalysisResourceHelper mlAnalysisResourceHelper;
    private final ExplainabilityRunHelper explainabilityRunHelper;
    private final EvaluationTableLoader tableLoader;
    private static final Logger LOG = LoggerFactory.getLogger(ModelEvaluationService.class);
    private static final int MAX_PAGE_SIZE = 10000;

    public ModelEvaluationService(ExperimentServiceFactory experimentServiceFactory,
            MlAnalysisResourceHelper mlAnalysisResourceHelper,
            ExplainabilityRunHelper explainabilityRunHelper,
            EvaluationTableLoader tableLoader,
            @Value("${app.mock.ml-evaluation.path-template:}") String mockEvaluationPathTemplate) {
        this.experimentServiceFactory = experimentServiceFactory;
        this.mlAnalysisResourceHelper = mlAnalysisResourceHelper;
        this.explainabilityRunHelper = explainabilityRunHelper;
        this.tableLoader = tableLoader;
        this.mockEvaluationPathTemplate = mockEvaluationPathTemplate;
    }

    /**
     * Full loader: pulls all 5 evaluation tables. Required only by /summary.
     * Each underlying table read is cached per CSV path by {@link EvaluationTableLoader},
     * so repeat calls don't reparse files even when this method itself isn't memoised.
     */
    public Optional<ModelEvaluationData> loadEvaluationData(String experimentId, String runId, String auth) {
        Optional<Map<String, String>> paths = resolvePaths(experimentId, runId, auth);
        if (paths.isEmpty()) return Optional.empty();
        Map<String, String> p = paths.get();

        Table xTest = tableLoader.loadTable(p.get("x_test"));
        Table yTest = tableLoader.loadTable(p.get("y_test"));
        Table yPred = tableLoader.loadTable(p.get("y_pred"));
        Table xTrain = tableLoader.loadTable(p.get("x_train"));
        Table yTrain = tableLoader.loadTable(p.get("y_train"));

        validateAlignment(xTest, yTest, yPred);
        return Optional.of(new ModelEvaluationData(xTest, yTest, yPred, xTrain, yTrain));
    }

    /**
     * Confusion matrix only needs the label columns. Skipping X_train (typically
     * the biggest CSV) and the feature matrix saves significant I/O.
     */
    public Optional<ModelEvaluationData> loadEvaluationDataForConfusionMatrix(
            String experimentId, String runId, String auth) {
        Optional<Map<String, String>> paths = resolvePaths(experimentId, runId, auth);
        if (paths.isEmpty()) return Optional.empty();
        Map<String, String> p = paths.get();

        Table yTest = tableLoader.loadTable(p.get("y_test"));
        Table yPred = tableLoader.loadTable(p.get("y_pred"));
        if (yTest.rowCount() != yPred.rowCount()) {
            throw new IllegalStateException("Row counts do not match between Y_test and Y_pred");
        }
        return Optional.of(new ModelEvaluationData(null, yTest, yPred, null, null));
    }

    /**
     * Test instances need the feature matrix and both label columns. Train tables
     * stay on disk.
     */
    public Optional<ModelEvaluationData> loadEvaluationDataForTestInstances(
            String experimentId, String runId, String auth) {
        Optional<Map<String, String>> paths = resolvePaths(experimentId, runId, auth);
        if (paths.isEmpty()) return Optional.empty();
        Map<String, String> p = paths.get();

        Table xTest = tableLoader.loadTable(p.get("x_test"));
        Table yTest = tableLoader.loadTable(p.get("y_test"));
        Table yPred = tableLoader.loadTable(p.get("y_pred"));
        validateAlignment(xTest, yTest, yPred);
        return Optional.of(new ModelEvaluationData(xTest, yTest, yPred, null, null));
    }

    /** Shared prelude: fetch & canonicalise file paths. */
    private Optional<Map<String, String>> resolvePaths(String experimentId, String runId, String auth) {
        Optional<Map<String, String>> rawPaths = explainabilityRunHelper.loadExplainabilityDataPaths(
                experimentId, runId, auth, "");
        if (rawPaths.isEmpty()) {
            LOG.warn("No file paths found for experimentId: {}, runId: {}", experimentId, runId);
            return Optional.empty();
        }
        Map<String, String> paths = normaliseKeys(rawPaths.get());
        assertContainsAll(paths);
        return Optional.of(paths);
    }

    private static final List<String> REQUIRED_KEYS = List.of(
            "x_test", "y_test", "y_pred", "x_train", "y_train");

    /**
     * Converts whatever keys MLflow returns (e.g. "X_test.csv", "x_test")
     * into a predictable, lower‑case, extension‑less map.
     */
    private Map<String, String> normaliseKeys(Map<String, String> raw) {
        return raw.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey()
                                .toLowerCase(Locale.ROOT)
                                .replace(".csv", ""),
                        Map.Entry::getValue));
    }

    /**
     * Throws if any required key is missing so we never pass null to Paths.get().
     */
    private void assertContainsAll(Map<String, String> m) {
        List<String> missing = REQUIRED_KEYS.stream()
                .filter(k -> !m.containsKey(k))
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "MLflow run is missing expected files: " + missing);
        }
    }

    private Path modelAnalysisResourceToPath(String filePath) {
        return Paths.get(filePath);
    }

    private void validateAlignment(Table x, Table y, Table yPred) {
        int n = x.rowCount();
        if (y.rowCount() != n || yPred.rowCount() != n) {
            throw new IllegalStateException("Row counts do not match between X_test, Y_test, and Y_pred");
        }
    }

    /**
     * Computes a confusion matrix from the evaluation data and returns it
     * as a structured result suitable for frontend consumption.
     * Converts labels to strings to support mixed types (e.g., numeric classes).
     *
     * @param data the evaluation data containing actual and predicted labels
     * @return a structured confusion matrix result
     */
    public ConfusionMatrixResult getConfusionMatrixResult(ModelEvaluationData data) {
        // Convert label columns to string format
        StringColumn actual = data.yTest().column(0).asStringColumn().setName("actual");
        StringColumn predicted = data.yPred().column(0).asStringColumn().setName("predicted");

        // Create a temporary table with both columns
        Table labelTable = Table.create("Labels", actual, predicted);

        // Compute the confusion matrix
        Table confusionTable = labelTable.xTabCounts("actual", "predicted");

        // skip total column
        List<String> predictedLabels = confusionTable.columnNames().subList(1, confusionTable.columnCount() - 1);

        List<List<Integer>> matrix = confusionTable.stream()
                .filter(row -> !row.getString(0).equalsIgnoreCase("Total")) // skip "Total" row
                .map(row -> predictedLabels.stream()
                        .map(label -> {
                            Object val = row.getObject(label);
                            return (val instanceof Number) ? ((Number) val).intValue() : 0;
                        })
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());

        return new ConfusionMatrixResult(predictedLabels, matrix);
    }

    /**
     * Returns a paged list of test instances with feature values,
     * actual labels, and predicted labels.
     *
     * @param data   the evaluation data containing X_test, Y_test, and Y_pred
     * @param offset the starting row index (nullable, defaults to 0)
     * @param limit  the maximum number of rows to return (nullable, defaults to
     *               100, capped)
     * @return a list of maps representing labeled test instances
     */
    /**
     * Stratified-by-confusion-cell sampler: groups rows by (actual, predicted) and
     * keeps up to `perCell` from each group. Caps the total at `maxRows`, dropping
     * from over-represented cells first (off-diagonal/misclassified cells are
     * preserved) so the response stays representative even on big test sets.
     */
    public List<Map<String, Object>> getLabeledTestInstancesStratified(
            ModelEvaluationData data, Integer perCellParam, Integer maxRowsParam) {
        int perCell = perCellParam != null && perCellParam > 0 ? perCellParam : 100;
        int maxRows = maxRowsParam != null && maxRowsParam > 0
                ? Math.min(maxRowsParam, MAX_PAGE_SIZE)
                : 2000;

        Table x = data.xTest();
        StringColumn actual = data.yTest().column(0).asStringColumn();
        StringColumn predicted = data.yPred().column(0).asStringColumn();
        List<String> featureNames = x.columnNames();
        int totalRows = x.rowCount();

        // Pass 1: bucket row indices by (actual, predicted), capped at perCell per cell.
        Map<String, List<Integer>> cellBuckets = new LinkedHashMap<>();
        for (int i = 0; i < totalRows; i++) {
            String key = actual.get(i) + " " + predicted.get(i);
            List<Integer> bucket = cellBuckets.computeIfAbsent(key, k -> new ArrayList<>());
            if (bucket.size() < perCell) {
                bucket.add(i);
            }
        }

        // Pass 2: gather indices, prioritising misclassified cells so the
        // global cap never drops error examples first.
        List<Integer> chosen = new ArrayList<>(Math.min(maxRows, totalRows));
        for (Map.Entry<String, List<Integer>> e : cellBuckets.entrySet()) {
            String[] parts = e.getKey().split(" ", 2);
            if (parts.length == 2 && !parts[0].equals(parts[1])) {
                chosen.addAll(e.getValue());
            }
        }
        for (Map.Entry<String, List<Integer>> e : cellBuckets.entrySet()) {
            String[] parts = e.getKey().split(" ", 2);
            if (parts.length == 2 && parts[0].equals(parts[1])) {
                chosen.addAll(e.getValue());
            }
        }
        if (chosen.size() > maxRows) {
            chosen = chosen.subList(0, maxRows);
        }
        // Emit in original row order so the UMAP layout stays stable across requests.
        chosen.sort(Integer::compareTo);
        return materialiseRows(x, actual, predicted, featureNames, chosen);
    }

    /**
     * Returns only misclassified rows (actual != predicted). Result is bounded
     * by `maxRows` (default 5000). Use when the user wants to deep-dive errors.
     */
    public List<Map<String, Object>> getMisclassifiedTestInstances(ModelEvaluationData data, Integer maxRowsParam) {
        int maxRows = maxRowsParam != null && maxRowsParam > 0
                ? Math.min(maxRowsParam, MAX_PAGE_SIZE)
                : 5000;

        Table x = data.xTest();
        StringColumn actual = data.yTest().column(0).asStringColumn();
        StringColumn predicted = data.yPred().column(0).asStringColumn();
        List<String> featureNames = x.columnNames();
        int totalRows = x.rowCount();

        List<Integer> chosen = new ArrayList<>();
        for (int i = 0; i < totalRows && chosen.size() < maxRows; i++) {
            if (!actual.get(i).equals(predicted.get(i))) {
                chosen.add(i);
            }
        }
        return materialiseRows(x, actual, predicted, featureNames, chosen);
    }

    private List<Map<String, Object>> materialiseRows(Table x, StringColumn actual, StringColumn predicted,
            List<String> featureNames, List<Integer> indices) {
        List<Map<String, Object>> rows = new ArrayList<>(indices.size());
        for (int i : indices) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (String feature : featureNames) {
                row.put(feature, x.column(feature).get(i));
            }
            row.put("actual", actual.get(i));
            row.put("predicted", predicted.get(i));
            rows.add(row);
        }
        return rows;
    }

    public List<Map<String, Object>> getLabeledTestInstances(ModelEvaluationData data, Integer offset, Integer limit) {
        Table x = data.xTest();
        StringColumn actual = data.yTest().column(0).asStringColumn();
        StringColumn predicted = data.yPred().column(0).asStringColumn();

        List<String> featureNames = x.columnNames();
        int totalRows = x.rowCount();

        int start = offset != null ? Math.max(0, offset) : 0;
        int end = Math.min(totalRows, start + (limit != null ? Math.min(limit, MAX_PAGE_SIZE) : 10000));

        List<Map<String, Object>> rows = new ArrayList<>(end - start);
        for (int i = start; i < end; i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (String feature : featureNames) {
                row.put(feature, x.column(feature).get(i));
            }
            row.put("actual", actual.get(i));
            row.put("predicted", predicted.get(i));
            rows.add(row);
        }

        return rows;
    }

    /**
     * Returns the ROC curve JSON data for a given run.
     * <p>
     * This method reads the ROC curve data from a JSON file located in the
     * <code>ml_analysis_resources</code> folder of the run.
     *
     * @param experimentId the ID of the experiment
     * @param runId        the ID of the run within the experiment
     * @return an Optional containing the ROC curve JSON data, or an empty Optional
     *         if not found
     */
    public Optional<String> getRocCurveData(String experimentId, String runId, String authorization) {
        Optional<Map<String, String>> filePaths = explainabilityRunHelper.loadExplainabilityDataPaths(experimentId,
                runId, authorization, "");
        // System.out.println("filePaths MODEL SERVICE: " + filePaths);
        ExperimentService service = experimentServiceFactory.getActiveService();
        if (service.getClass().getSimpleName().equals("MLflowExperimentService")) {
            Path rocPath = modelAnalysisResourceToPath(filePaths.get().get("roc_data"));
            if (!Files.exists(rocPath)) {
            return Optional.empty();
        }

        try {
            return Optional.of(Files.readString(rocPath));
        } catch (IOException e) {
            LOG.error("Error reading ROC curve file", e);
            return Optional.empty();
        }

        } else {
                Path rocPath = modelAnalysisResourceToPath(filePaths.get().get("rocdata"));
                if (!Files.exists(rocPath)) {
            return Optional.empty();
        }

        try {
            return Optional.of(Files.readString(rocPath));
        } catch (IOException e) {
            LOG.error("Error reading ROC curve file", e);
            return Optional.empty();
        }

           
        }
        
    }

    /*
     * Loads the paths of the required files for explainability analysis.
     * <p>
     * This method checks if the specified experiment and run have the necessary
     * files for explainability analysis.
     *
     * @param experimentId the ID of the experiment
     * 
     * @param runId the ID of the run within the experiment
     * 
     * @return an Optional containing a map of file names to their paths, or an
     * empty
     * Optional if no files are found
     */
    @Cacheable(value = "explainabilityDataPaths", key = "#experimentId + '::' + #runId")
    public Optional<Map<String, Path>> loadExplainabilityDataPaths(String experimentId, String runId) {
        LOG.info("Loading evaluation data for experimentId: {}, runId: {}", experimentId, runId);
        ExperimentService service = experimentServiceFactory.getActiveService();
        ResponseEntity<Run> response = service.getRunById(experimentId, runId);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return Optional.empty();
        }

        Run run = response.getBody();
        Optional<Path> folderOpt = resolveMlAnalysisFolderPath(run);
        if (folderOpt.isEmpty()) {
            return Optional.empty();
        }

        Path folder = folderOpt.get();

        if (!mlAnalysisResourceHelper.hasRequiredFiles(folder)) {
            LOG.warn("Analysis folder exists but is missing one or more required files.");
            return Optional.empty();
        }

        if (mlAnalysisResourceHelper.getRequiredFilePaths(folder).isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(mlAnalysisResourceHelper.getRequiredFilePaths(folder));
        }
    }

    /**
     * Computes an evaluation summary for a trained model using test and train data.
     * <p>
     * This includes:
     * <ul>
     * <li>Global scalar metrics (true global/micro-averaged): accuracy, precision,
     * recall, F1</li>
     * <li>Per-class classification metrics</li>
     * <li>Input shape: number of samples and features in X_test</li>
     * <li>Dataset size breakdown from X_train and X_test</li>
     * <li>Class labels</li>
     * </ul>
     *
     * @param data Evaluation data including X_test, Y_test, Y_pred, and X_train
     * @return a {@link ModelEvaluationSummary} with structured results for display
     *         and analysis
     */
    public ModelEvaluationSummary getModelEvaluationSummary(ModelEvaluationData data) {
        Table xTest = data.xTest();
        Table yTest = data.yTest();
        Table yPred = data.yPred();
        Table xTrain = data.xTrain();

        int numSamples = xTest.rowCount();
        int numFeatures = xTest.columnCount();
        int trainSize = xTrain.rowCount();

        StringColumn actual = yTest.column(0).asStringColumn();
        StringColumn predicted = yPred.column(0).asStringColumn();

        // Single pass: tally TP / FP / FN / support per label, plus correct count.
        // Previous version was O(n * k) — scanning both columns once per class.
        Map<String, int[]> stats = new HashMap<>(); // label -> {tp, fp, fn, support}
        int correct = 0;
        int n = actual.size();
        for (int i = 0; i < n; i++) {
            String a = actual.get(i);
            String pLabel = predicted.get(i);
            int[] aStats = stats.computeIfAbsent(a, k -> new int[4]);
            aStats[3]++; // support
            if (a.equals(pLabel)) {
                aStats[0]++; // tp
                correct++;
            } else {
                aStats[2]++; // fn for the actual class
                int[] pStats = stats.computeIfAbsent(pLabel, k -> new int[4]);
                pStats[1]++; // fp for the predicted class
            }
        }

        // Deterministic order: TableSaw's unique() defines the canonical label order
        // for the response (matches confusion-matrix axis labels).
        List<String> classLabels = actual.unique().asList();
        List<ClassReportEntry> classReport = new ArrayList<>(classLabels.size());
        int totalTP = 0, totalFP = 0, totalFN = 0;
        for (String label : classLabels) {
            int[] s = stats.getOrDefault(label, new int[4]);
            int tp = s[0], fp = s[1], fn = s[2], support = s[3];
            totalTP += tp;
            totalFP += fp;
            totalFN += fn;
            double precision = (tp + fp) == 0 ? Double.NaN : (double) tp / (tp + fp);
            double recall = support == 0 ? Double.NaN : (double) tp / support;
            double f1 = precision + recall == 0 ? Double.NaN : 2 * precision * recall / (precision + recall);
            classReport.add(new ClassReportEntry(label, precision, recall, f1, support));
        }

        double precision = totalTP + totalFP == 0 ? Double.NaN : (double) totalTP / (totalTP + totalFP);
        double recall = totalTP + totalFN == 0 ? Double.NaN : (double) totalTP / (totalTP + totalFN);
        double f1 = precision + recall == 0 ? Double.NaN : 2 * precision * recall / (precision + recall);
        double accuracy = numSamples == 0 ? Double.NaN : (double) correct / numSamples;

        OverallMetrics metrics = new OverallMetrics(accuracy, precision, recall, f1);
        Map<String, Integer> splitSizes = Map.of("train", trainSize, "test", numSamples);

        return new ModelEvaluationSummary(
                metrics,
                classReport,
                numSamples,
                numFeatures,
                classLabels,
                splitSizes);
    }

    // private Optional<Path> resolveMlAnalysisFolderPath(Run run) {
    // // return mlAnalysisResourceHelper.getMlResourceFolder(run);
    // return Optional.of(Paths.get(mockEvaluationPath));
    // }
    private Optional<Path> resolveMlAnalysisFolderPath(Run run) {
        String pathStr = mockEvaluationPathTemplate
                .replace("{experimentId}", run.getExperimentId())
                .replace("{runId}", run.getId());
        return Optional.of(Paths.get(pathStr));
    }

}
