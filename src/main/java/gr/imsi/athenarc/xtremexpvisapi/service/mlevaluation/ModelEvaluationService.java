package gr.imsi.athenarc.xtremexpvisapi.service.mlevaluation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import gr.imsi.athenarc.xtremexpvisapi.datasource.CsvDataSource;
import gr.imsi.athenarc.xtremexpvisapi.datasource.DataSourceFactory;
import gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.SourceType;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Run;
import gr.imsi.athenarc.xtremexpvisapi.domain.mlevaluation.ConfusionMatrixResult;
import gr.imsi.athenarc.xtremexpvisapi.domain.mlevaluation.ModelEvaluationSummary;
import gr.imsi.athenarc.xtremexpvisapi.domain.mlevaluation.ModelEvaluationSummary.ClassReportEntry;
import gr.imsi.athenarc.xtremexpvisapi.domain.mlevaluation.ModelEvaluationSummary.OverallMetrics;
import gr.imsi.athenarc.xtremexpvisapi.service.ExperimentService;
import gr.imsi.athenarc.xtremexpvisapi.service.ExperimentServiceFactory;
import gr.imsi.athenarc.xtremexpvisapi.service.shared.MlAnalysisResourceHelper;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

@Service
public class ModelEvaluationService {

    private final DataSourceFactory dataSourceFactory;
    private final ExperimentServiceFactory experimentServiceFactory;

    private static final Logger LOG = LoggerFactory.getLogger(ModelEvaluationService.class);

    // Maximum number of rows to return for labeled test instances
    private static final int MAX_PAGE_SIZE = 10000;

    @Value("${app.mock.ml-evaluation.path:}")
    private String mockEvaluationPath;

    private final MlAnalysisResourceHelper mlAnalysisResourceHelper;

    public ModelEvaluationService(DataSourceFactory dataSourceFactory,
            ExperimentServiceFactory experimentServiceFactory, MlAnalysisResourceHelper mlAnalysisResourceHelper) {
        this.dataSourceFactory = dataSourceFactory;
        this.experimentServiceFactory = experimentServiceFactory;
        this.mlAnalysisResourceHelper = mlAnalysisResourceHelper;
    }

    @Cacheable(value = "modelEvaluationData", key = "#experimentId + '::' + #runId")
    public Optional<ModelEvaluationData> loadEvaluationData(String experimentId, String runId) {
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
        Table xTest = loadTable(mlAnalysisResourceHelper.getXTestPath(folder));
        Table yTest = loadTable(mlAnalysisResourceHelper.getYTestPath(folder));
        Table yPred = loadTable(mlAnalysisResourceHelper.getYPredPath(folder));
        Table xTrain = loadTable(mlAnalysisResourceHelper.getXTrainPath(folder));
        Table yTrain = loadTable(mlAnalysisResourceHelper.getYTrainPath(folder));

        validateAlignment(xTest, yTest, yPred);
        return Optional.of(new ModelEvaluationData(xTest, yTest, yPred, xTrain, yTrain));
    }

    private Table loadTable(Path path) {
        SourceType type = SourceType.csv;
        CsvDataSource ds = (CsvDataSource) dataSourceFactory.createDataSource(type, path.toString());
        if (ds == null) {
            throw new IllegalStateException("Failed to create data source for path: " + path);
        }
        LOG.info("Loading ML evaluation table from path: {}", path);
        return ds.readCsvFromFile(path);
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
    public Optional<String> getRocCurveData(String experimentId, String runId) {
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

        Path rocPath = mlAnalysisResourceHelper.getRocCurvePath(folderOpt.get());
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

        List<String> classLabels = actual.unique().asList();
        List<ClassReportEntry> classReport = new ArrayList<>();

        int totalTP = 0, totalFP = 0, totalFN = 0;

        // Per-class classification report
        for (String label : classLabels) {
            int tp = 0, fp = 0, fn = 0, support = 0;
            for (int i = 0; i < actual.size(); i++) {
                String actualLabel = actual.get(i);
                String predictedLabel = predicted.get(i);

                if (actualLabel.equals(label)) {
                    support++;
                    if (predictedLabel.equals(label))
                        tp++;
                    else
                        fn++;
                } else if (predictedLabel.equals(label)) {
                    fp++;
                }
            }

            totalTP += tp;
            totalFP += fp;
            totalFN += fn;

            double precision = tp + fp == 0 ? Double.NaN : (double) tp / (tp + fp);
            double recall = support == 0 ? Double.NaN : (double) tp / support;
            double f1 = precision + recall == 0 ? Double.NaN : 2 * precision * recall / (precision + recall);

            classReport.add(new ClassReportEntry(label, precision, recall, f1, support));
        }

        // Global scalar metrics
        double precision = totalTP + totalFP == 0 ? Double.NaN : (double) totalTP / (totalTP + totalFP);
        double recall = totalTP + totalFN == 0 ? Double.NaN : (double) totalTP / (totalTP + totalFN);
        double f1 = precision + recall == 0 ? Double.NaN : 2 * precision * recall / (precision + recall);

        // Accuracy: count all correct predictions
        int correct = 0;
        for (int i = 0; i < actual.size(); i++) {
            if (actual.get(i).equals(predicted.get(i))) {
                correct++;
            }
        }
        double accuracy = (double) correct / numSamples;

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

    private Optional<Path> resolveMlAnalysisFolderPath(Run run) {
        // return mlAnalysisResourceHelper.getMlResourceFolder(run);
        return Optional.of(Paths.get(mockEvaluationPath));
    }

}
