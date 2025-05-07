package gr.imsi.athenarc.xtremexpvisapi.service.explainability;

import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Run;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Param;
import gr.imsi.athenarc.xtremexpvisapi.service.ExperimentService;
import gr.imsi.athenarc.xtremexpvisapi.service.ExperimentServiceFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

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
public class ExplainabilityRunHelper {

    private final ExperimentServiceFactory experimentServiceFactory;

    public ExplainabilityRunHelper(ExperimentServiceFactory experimentServiceFactory) {
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
    public List<Run> findSimilarRuns(Run referenceRun) {
        Set<String> referenceKeys = getParamNames(referenceRun);

        if (referenceKeys.isEmpty()) {
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
        if (run.getParams() == null)
            return Set.of();
        return run.getParams().stream()
                .map(Param::getName)
                .collect(Collectors.toSet());
    }
}
