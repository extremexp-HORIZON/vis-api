package gr.imsi.athenarc.xtremexpvisapi.service.shared;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.DataAsset;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Run;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv2.params.DataSource;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv2.params.SourceType;
import gr.imsi.athenarc.xtremexpvisapi.service.experiment.ExperimentServiceFactory;
import gr.imsi.athenarc.xtremexpvisapi.service.files.FileService;

/**
 * Helper for resolving and validating ML analysis resources
 * associated with a run's data assets (e.g., X_test.csv, model, etc.).
 */
@Component
public class MlAnalysisResourceHelper {

    private String mlAnalysisFolderName;
    private final List<String> supportedModelExtensions = List.of(".pkl", ".pt"); // Configurable list of supported
                                                                                  // model extensions

    private final String mlEvaluationPath;
    private final FileService fileService;
    private final ExperimentServiceFactory experimentServiceFactory;

    @Autowired
    public MlAnalysisResourceHelper(@Value("${app.working.directory}") String mlEvaluationPath,
            FileService fileService,
            ExperimentServiceFactory experimentServiceFactory) {
        this.mlEvaluationPath = mlEvaluationPath;
        this.fileService = fileService;
        this.experimentServiceFactory = experimentServiceFactory;
        this.mlAnalysisFolderName = resolveAnalysisFolderName();
    }

    private String resolveAnalysisFolderName() {
        String serviceName = experimentServiceFactory.getActiveService().getClass().getSimpleName();
        return serviceName.equalsIgnoreCase("MLflowExperimentService") ? "explainability" : "MLAnalysis";
    }

    public String getMlAnalysisFolderName() {
        return mlAnalysisFolderName;
    }

    /**
     * Returns the path to the ML analysis resources folder for a given run.
     *
     * @param run the run to check
     * @return the path to the ML analysis resources folder, or empty if not found
     */
    public Optional<Map<String, String>> getRequiredFilePaths(Run run, String authorization, String explanationType) {
        // Filter the data assets to find the one with the ML analysis folder name

        String folderName = getMlAnalysisFolderName(); // or just use mlAnalysisFolderName directly if preferred

        List<DataAsset> filesPath = run.getDataAssets().stream()
                .filter(a -> folderName.equalsIgnoreCase(a.getFolder()))
                .toList();
        // Check if the run has all required files for ML analysis
        if (!hasFiles(filesPath)) {
            return Optional.empty();
        }

        Map<String, String> requiredFilePaths = new LinkedHashMap<>();
        filesPath.forEach(dataAsset -> {
            // Skip model files unless explanationType is "hyperparameter"
            String assetNameWithoutExtension = dataAsset.getName().substring(0, dataAsset.getName().lastIndexOf("."))
                    .toLowerCase();
            if (!"model".equals(assetNameWithoutExtension) && "hyperparameter".equals(explanationType)
                    || explanationType == null) {
                return; // Skip this asset
            }

            if (dataAsset.getSourceType() == SourceType.local) {
                requiredFilePaths.put(dataAsset.getName(), dataAsset.getSource());
            } else {
                DataSource dataSource = new DataSource();
                dataSource.setSource(dataAsset.getSource());
                dataSource.setSourceType(dataAsset.getSourceType());
                dataSource.setFormat(dataAsset.getFormat());
                dataSource.setFileName(dataAsset.getName());
                dataSource.setRunId(run.getId());
                try {
                    String filePath = fileService.downloadAndCacheDataAsset(run.getId(), dataSource, authorization);
                    requiredFilePaths.put(assetNameWithoutExtension, filePath);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to download data asset: " + dataAsset.getName(), e);
                }
            }

        });
        return Optional.of(requiredFilePaths);
    }

    private boolean hasFiles(List<DataAsset> dataAssets) {
        List<String> requiredFiles = List.of("X_test.csv", "Y_test.csv", "Y_train.csv", "X_train.csv", "Y_pred.csv");
        boolean hasRequiredFiles = requiredFiles.stream()
                .allMatch(fileName -> dataAssets.stream()
                        .anyMatch(asset -> asset.getName().equalsIgnoreCase(fileName)));

        // Check for any supported model file
        boolean hasModelFile = dataAssets.stream()
                .anyMatch(asset -> {
                    String assetName = asset.getName().toLowerCase();
                    return assetName.startsWith("model.") &&
                            supportedModelExtensions.stream()
                                    .anyMatch(ext -> assetName.endsWith(ext));
                });

        return hasRequiredFiles && hasModelFile;
    }

    /**
     * Returns the path to the ML analysis resources folder for a given run.
     *
     * @param run the run to check
     * @return the path to the ML analysis resources folder, or empty if not found
     */
    public Optional<Path> getMlResourceFolder(Run run) {
        Optional<Path> filesPath = run.getDataAssets().stream()
                .filter(a -> getMlAnalysisFolderName().equals(a.getName()))
                .map(a -> Paths.get(a.getSource()))
                .findFirst();
        if (filesPath.isPresent()) {
            // Transform the path to the server ml-evaluation folder
            String pathStr = filesPath.get().toString().replace("workspace/datasets/output", mlEvaluationPath)
                    .replace("**", "") + run.getId();
            return Optional.of(Paths.get(pathStr));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Checks that all required evaluation files exist in the folder.
     *
     * @param folder the ml_analysis_resources folder
     * @return true if all required files are present
     */
    public boolean hasRequiredFiles(Path folder) {
        boolean hasRequiredDataFiles = findFileIgnoreCase(folder, "X_test.csv").isPresent() &&
                findFileIgnoreCase(folder, "Y_test.csv").isPresent() &&
                findFileIgnoreCase(folder, "Y_pred.csv").isPresent() &&
                findFileIgnoreCase(folder, "X_train.csv").isPresent() &&
                findFileIgnoreCase(folder, "Y_train.csv").isPresent();

        // Check for any supported model file
        boolean hasModelFile = findModelFile(folder).isPresent();

        return hasRequiredDataFiles && hasModelFile;
        // findFileIgnoreCase(folder, "roc_data.json").isPresent();
    }

    private Optional<Path> findModelFile(Path folder) {
        try {
            return Files.list(folder)
                    .filter(p -> {
                        String fileName = p.getFileName().toString().toLowerCase();
                        return fileName.startsWith("model.") &&
                                supportedModelExtensions.stream()
                                        .anyMatch(ext -> fileName.endsWith(ext));
                    })
                    .findFirst();
        } catch (Exception e) {
            return Optional.empty();
        }
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
        // map.put("roc_curve", findFileIgnoreCase(folder,
        // "roc_data.json").orElse(null));
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