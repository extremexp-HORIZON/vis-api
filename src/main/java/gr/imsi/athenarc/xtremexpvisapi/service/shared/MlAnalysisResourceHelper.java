package gr.imsi.athenarc.xtremexpvisapi.service.shared;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Run;

/**
 * Helper for resolving and validating ML analysis resources
 * associated with a run's data assets (e.g., X_test.csv, model, etc.).
 */
@Component
public class MlAnalysisResourceHelper {

    private static final String ML_ANALYSIS_FOLDER_NAME = "ml_analysis_resources";

    public Optional<Path> getMlResourceFolder(Run run) {
        return run.getDataAssets().stream()
                .filter(a -> ML_ANALYSIS_FOLDER_NAME.equals(a.getName()))
                .map(a -> Paths.get(a.getSource()))
                .findFirst();
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

    public Path getModelPath1(Path folder) {
        return folder.resolve("model1.pkl");
    }

    public Path getModelPath2(Path folder) {
        return folder.resolve("model2.pkl");
    }

    public Path getModelPath3(Path folder) {
        return folder.resolve("model3.pkl");
    }

    public Path getModelPath4(Path folder) {
        return folder.resolve("model4.pkl");
    }

    public Path getModelPath5(Path folder) {
        return folder.resolve("model5.pkl");
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
        return Files.exists(getXTestPath(folder)) &&
                Files.exists(getYTestPath(folder)) &&
                Files.exists(getYPredPath(folder)) &&
                Files.exists(getXTrainPath(folder)) &&
                Files.exists(getYTrainPath(folder));
    }

    /**
     * Returns a map of named required resources and their paths.
     *
     * @param folder the ml_analysis_resources folder
     * @return map of logical names to resolved paths
     */
    public Map<String, Path> getRequiredFilePaths(Path folder) {
        Map<String, Path> map = new LinkedHashMap<>();
        map.put("X_test", getXTestPath(folder));
        map.put("Y_test", getYTestPath(folder));
        map.put("Y_train", getYTrainPath(folder));
        map.put("X_train", getXTrainPath(folder));
        map.put("Y_pred", getYPredPath(folder));
        map.put("model1", getModelPath1(folder));
        map.put("model2", getModelPath2(folder));
        map.put("model3", getModelPath3(folder));
        map.put("model4", getModelPath4(folder));
        map.put("model5", getModelPath5(folder));
        return map;
    }
}
