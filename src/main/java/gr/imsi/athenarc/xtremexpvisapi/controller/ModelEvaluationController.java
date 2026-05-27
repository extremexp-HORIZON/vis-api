package gr.imsi.athenarc.xtremexpvisapi.controller;

import gr.imsi.athenarc.xtremexpvisapi.domain.mlevaluation.ConfusionMatrixResult;
import gr.imsi.athenarc.xtremexpvisapi.domain.mlevaluation.ModelEvaluationSummary;
import gr.imsi.athenarc.xtremexpvisapi.service.mlevaluation.ModelEvaluationService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/experiments/{experimentId}/runs/{runId}/evaluation")
public class ModelEvaluationController {

  private final ModelEvaluationService evaluationService;

  public ModelEvaluationController(ModelEvaluationService evaluationService) {
    this.evaluationService = evaluationService;
  }

  /**
   * Returns a summary of model evaluation metrics for a completed run.
   *
   * <p>This includes: - Overall accuracy, precision, recall, F1 (global, micro-averaged) -
   * Per-class classification report - Test set shape and class distribution
   *
   * <p>Requires the files: X_test.csv, Y_test.csv, Y_pred.csv, X_train.csv
   *
   * @param experimentId the ID of the experiment
   * @param runId the ID of the run
   * @return a {@link ModelEvaluationSummary} if available, or 404 otherwise
   */
  @GetMapping("/summary")
  public ResponseEntity<ModelEvaluationSummary> getModelEvaluationSummary(
      @PathVariable String experimentId,
      @PathVariable String runId,
      @RequestHeader(value = "Authorization", required = false) String authorization) {

    return evaluationService
        .loadEvaluationData(experimentId, runId, authorization)
        .map(evaluationService::getModelEvaluationSummary)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Returns the confusion matrix for a completed run, if evaluation resources are available.
   *
   * <p>This endpoint assumes the run has stored the necessary files (e.g., <code>X_test.csv</code>,
   * <code>Y_test.csv</code>, and <code>Y_pred.csv</code>). The confusion matrix compares ground
   * truth vs predicted labels for classification tasks.
   *
   * @param experimentId the ID of the experiment
   * @param runId the ID of the run within the experiment
   * @return a {@link ConfusionMatrixResult} if evaluation data is available, or 404 otherwise
   */
  @GetMapping("/confusion-matrix")
  public ResponseEntity<ConfusionMatrixResult> getConfusionMatrix(
      @PathVariable String experimentId,
      @PathVariable String runId,
      @RequestHeader(value = "Authorization", required = false) String authorization) {

    // Only y_test + y_pred are needed; X_train/X_test/Y_train stay on disk.
    return evaluationService
        .loadEvaluationDataForConfusionMatrix(experimentId, runId, authorization)
        .map(evaluationService::getConfusionMatrixResult)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Returns a paginated list of labeled test instances for a given run, including input feature
   * values, actual (ground truth) labels, and model predictions.
   *
   * <p>Requires that the run has produced the files <code>X_test.csv</code>, <code>Y_test.csv
   * </code>, and <code>Y_pred.csv</code>.
   *
   * @param experimentId the ID of the experiment
   * @param runId the ID of the run within the experiment
   * @param offset the index of the first row to return (optional, default is 0)
   * @param limit the maximum number of rows to return (optional, default is 100, capped)
   * @return a list of labeled test instances, or 404 if required data is missing
   */
  /**
   * Test instances sampler.
   *
   * <p>{@code strategy} chooses which rows are returned:
   *
   * <ul>
   *   <li><b>first</b> (default — backward compatible): rows {@code [offset, offset+limit)}.
   *   <li><b>stratified</b>: groups by (actual, predicted) and keeps up to {@code perCell} rows per
   *       confusion cell, capped by {@code maxRows}. Misclassified cells are preserved first so
   *       errors are never dropped by the cap.
   * </ul>
   *
   * Setting {@code misclassifiedOnly=true} short-circuits to only the rows where {@code actual !=
   * predicted}.
   */
  @GetMapping("/test-instances")
  public ResponseEntity<List<Map<String, Object>>> getLabeledTestInstances(
      @PathVariable String experimentId,
      @PathVariable String runId,
      @RequestParam(required = false) Integer offset,
      @RequestParam(required = false) Integer limit,
      @RequestParam(required = false, defaultValue = "first") String strategy,
      @RequestParam(required = false) Integer perCell,
      @RequestParam(required = false) Integer maxRows,
      @RequestParam(required = false, defaultValue = "false") boolean misclassifiedOnly,
      @RequestHeader(value = "Authorization", required = false) String authorization) {

    return evaluationService
        .loadEvaluationDataForTestInstances(experimentId, runId, authorization)
        .map(
            data -> {
              if (misclassifiedOnly) {
                return evaluationService.getMisclassifiedTestInstances(data, maxRows);
              }
              if ("stratified".equalsIgnoreCase(strategy)) {
                return evaluationService.getLabeledTestInstancesStratified(data, perCell, maxRows);
              }
              return evaluationService.getLabeledTestInstances(data, offset, limit);
            })
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Returns the ROC curve data for a given run.
   *
   * <p>This endpoint reads the ROC curve data from a JSON file located in the <code>
   * ml_analysis_resources</code> folder of the run.
   *
   * @param experimentId the ID of the experiment
   * @param runId the ID of the run within the experiment
   * @return a JSON string representing the ROC curve data, or 404 if not found
   */
  @GetMapping("/roc-curve")
  public ResponseEntity<String> getRocCurveData(
      @PathVariable String experimentId,
      @PathVariable String runId,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return evaluationService
        .getRocCurveData(experimentId, runId, authorization)
        .map(json -> ResponseEntity.ok().body(json))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
