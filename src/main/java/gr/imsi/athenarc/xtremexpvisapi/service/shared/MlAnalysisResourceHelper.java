package gr.imsi.athenarc.xtremexpvisapi.service.shared;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Run;

/**
 * Helper for resolving and validating ML analysis resources
 * associated with a run's data assets (e.g., X_test.csv, model, etc.).
 */
@Component
public class MlAnalysisResourceHelper {

    private static final String ML_ANALYSIS_FOLDER_NAME = "MLAnalysisResources";
    
    @Value("${app.working.directory}")
    private String mlEvaluationPath;


    /**
     * Returns the path to the ML analysis resources folder for a given run.
     *
     * @param run the run to check
     * @return the path to the ML analysis resources folder, or empty if not found
     */
    public Optional<Path> getMlResourceFolder(Run run) {
        Optional<Path> filesPath = run.getDataAssets().stream()
                .filter(a -> ML_ANALYSIS_FOLDER_NAME.equals(a.getName()))
                .map(a -> Paths.get(a.getSource()))
                .findFirst();
        if (filesPath.isPresent()) {
            // Transform the path to the server ml-evaluation folder
            String pathStr = filesPath.get().toString().replace("workspace/datasets/output", mlEvaluationPath).replace("**", "") + run.getId();
            return Optional.of(Paths.get(pathStr));
        } else {
            return Optional.empty();
        }
    }

    public boolean hasMlAnalysisResources(Run run) {
        return getMlResourceFolder(run).isPresent();
    }

    public Path getXTestPath(Path folder) {
        return folder.resolve("X_test.csv");
    }

    public Path getXTrainPath(Path folder) {
        return folder.resolve("X_train.csv");
    }

    public Path getYTrainPath(Path folder) {
        return folder.resolve("Y_train.csv");
    }

    public Path getYTestPath(Path folder) {
        return folder.resolve("Y_test.csv");
    }

    public Path getYPredPath(Path folder) {
        return folder.resolve("Y_pred.csv");
    }

    public Path getModelPath(Path folder) {
        return folder.resolve("model.pkl");
    }

    public Path getRocCurvePath(Path folder) {
        return folder.resolve("roc_data.json");
    }

    /**
     * Checks that all required evaluation files exist in the folder.
     *
     * @param folder the ml_analysis_resources folder
     * @return true if all required files are present
     */
    public boolean hasRequiredFiles(Path folder) {
    return findFileIgnoreCase(folder, "X_test.csv").isPresent() &&
           findFileIgnoreCase(folder, "Y_test.csv").isPresent() &&
           findFileIgnoreCase(folder, "Y_pred.csv").isPresent() &&
           findFileIgnoreCase(folder, "X_train.csv").isPresent() &&
           findFileIgnoreCase(folder, "Y_train.csv").isPresent() &&
           findFileIgnoreCase(folder, "model.pkl").isPresent() &&
           findFileIgnoreCase(folder, "roc_data.json").isPresent();
}

    /**
     * Returns a map of named required resources and their paths.
     *
     * @param folder the ml_analysis_resources folder
     * @return map of logical names to resolved paths
     */
    public Map<String, Path> getRequiredFilePaths(Path folder) {
    Map<String, Path> map = new LinkedHashMap<>();
    map.put("X_test", findFileIgnoreCase(folder, "X_test.csv").orElse(null));
    map.put("Y_test", findFileIgnoreCase(folder, "Y_test.csv").orElse(null));
    map.put("Y_train", findFileIgnoreCase(folder, "Y_train.csv").orElse(null));
    map.put("X_train", findFileIgnoreCase(folder, "X_train.csv").orElse(null));
    map.put("Y_pred", findFileIgnoreCase(folder, "Y_pred.csv").orElse(null));
    map.put("model", findFileIgnoreCase(folder, "model.pkl").orElse(null));
    map.put("roc_curve", findFileIgnoreCase(folder, "roc_data.json").orElse(null));
    return map;
}




private Optional<Path> findFileIgnoreCase(Path folder, String fileName) {
    try {
        return Files.list(folder)
                .filter(p -> p.getFileName().toString().equalsIgnoreCase(fileName))
                .findFirst();
    } catch (Exception e) {
        return Optional.empty();
    }
}
}
