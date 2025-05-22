package gr.imsi.athenarc.xtremexpvisapi.domain.mlevaluation;

import java.util.List;
import java.util.Map;

/**
 * Summary of model evaluation metrics and metadata.
 * Structured to align with standard scikit-learn terminology.
 */
public record ModelEvaluationSummary(

        /**
         * Micro-averaged or global scalar metrics (precision, recall, F1, accuracy).
         */
        OverallMetrics overallMetrics,

        /**
         * Per-class metrics as in scikit-learn's classification_report.
         */
        List<ClassReportEntry> classificationReport,

        /**
         * Number of rows in X_test.
         */
        int numSamples,

        /**
         * Number of features in X_test.
         */
        int numFeatures,

        /**
         * Unique class labels.
         */
        List<String> classLabels,

        /**
         * Dataset size breakdown, e.g. {"train": 600, "test": 200}
         */
        Map<String, Integer> dataSplitSizes) {
    public record OverallMetrics(double accuracy, double precision, double recall, double f1Score) {
    }

    public record ClassReportEntry(String label, double precision, double recall, double f1Score, int support) {
    }
}
