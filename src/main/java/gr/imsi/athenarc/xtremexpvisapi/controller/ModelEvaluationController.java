package gr.imsi.athenarc.xtremexpvisapi.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import gr.imsi.athenarc.xtremexpvisapi.service.mlevaluation.ModelEvaluationService;
import gr.imsi.athenarc.xtremexpvisapi.service.mlevaluation.ConfusionMatrixResult;

@RestController
@RequestMapping("/experiments/{experimentId}/runs/{runId}/evaluation")
public class ModelEvaluationController {

    private final ModelEvaluationService evaluationService;

    public ModelEvaluationController(ModelEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    /**
     * Returns the confusion matrix for a completed run, if evaluation resources are
     * available.
     * <p>
     * This endpoint assumes the run has stored the necessary files (e.g.,
     * <code>X_test.csv</code>,
     * <code>Y_test.csv</code>, and <code>Y_pred.csv</code>).
     * The confusion matrix compares ground truth vs predicted labels for
     * classification tasks.
     *
     * @param experimentId the ID of the experiment
     * @param runId        the ID of the run within the experiment
     * @return a {@link ConfusionMatrixResult} if evaluation data is available, or
     *         404 otherwise
     */
    @GetMapping("/confusion-matrix")
    public ResponseEntity<ConfusionMatrixResult> getConfusionMatrix(
            @PathVariable String experimentId,
            @PathVariable String runId) {

        return evaluationService.loadEvaluationData(experimentId, runId)
                .map(evaluationService::getConfusionMatrixResult)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Returns a paginated list of labeled test instances for a given run,
     * including input feature values, actual (ground truth) labels, and model
     * predictions.
     * <p>
     * Requires that the run has produced the files <code>X_test.csv</code>,
     * <code>Y_test.csv</code>, and <code>Y_pred.csv</code>.
     *
     * @param experimentId the ID of the experiment
     * @param runId        the ID of the run within the experiment
     * @param offset       the index of the first row to return (optional, default
     *                     is 0)
     * @param limit        the maximum number of rows to return (optional, default
     *                     is 100, capped)
     * @return a list of labeled test instances, or 404 if required data is missing
     */
    @GetMapping("/test-instances")
    public ResponseEntity<List<Map<String, Object>>> getLabeledTestInstances(
            @PathVariable String experimentId,
            @PathVariable String runId,
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Integer limit) {

        return evaluationService.loadEvaluationData(experimentId, runId)
                .map(data -> evaluationService.getLabeledTestInstances(data, offset, limit))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}
