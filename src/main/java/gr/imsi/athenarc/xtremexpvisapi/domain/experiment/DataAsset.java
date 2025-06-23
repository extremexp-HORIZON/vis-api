package gr.imsi.athenarc.xtremexpvisapi.domain.experiment;

import java.util.Map;

import lombok.Data;

/**
 * Represents an input dataset or output artifact used in an experiment.
 */
@Data
public class DataAsset {

    /**
     * Enum representing the possible roles of a DataAsset.
     * Defines whether the asset is an input dataset or an output artifact.
     */
    public enum Role {
        INPUT, // Represents input datasets
        OUTPUT // Represents output artifacts
    }

    public enum SourceType {
        http,
        local
    }

    /**
     * The name of the data asset.
     */
    private String name;

    /**
     * The type of the data source (e.g., "http", "local").
     */
    private SourceType sourceType;

    /**
     * The exact location of the asset (e.g., "http://datasets/train.csv",
     * "file:///models/model.pkl").
     */
    private String source;

    /**
     * The file format of the asset (e.g., "csv", "json", "parquet", "pkl",
     * "image").
     * This field is optional.
     */
    private String format;

    /**
     * Specifies whether the asset is an INPUT dataset or an OUTPUT artifact.
     * This field is optional.
     */
    private Role role;

    /**
     * The task this asset is related to, if applicable.
     * This field is optional.
     */
    private String task;

    /**
     * Logical folder or catalog this data asset belongs to.
     * This field is used to group multiple file-level assets under a virtual
     * folder.
     * This does not imply that the data asset is a folder itself. If the asset
     * represents a real folder (e.g., a directory on the file system), this field
     * should be {@code null}.
     */
    private String folder;

    /**
     * Additional metadata related to the data asset, stored as key-value pairs.
     * This field is optional.
     */
    private Map<String, String> tags;

    // Constructors

    public DataAsset() {
    }

    public DataAsset(String name, SourceType sourceType, String source, String format,
            Role role, String task, Map<String, String> tags, String folder) {
        this.name = name;
        this.sourceType = sourceType;
        this.source = source;
        this.format = format;
        this.role = role;
        this.task = task;
        this.tags = tags;
        this.folder = folder;
    }

    @Override
    public String toString() {
        return "DataAsset{" +
                "name='" + name + '\'' +
                ", sourceType='" + sourceType + '\'' +
                ", source='" + source + '\'' +
                ", format='" + format + '\'' +
                ", role=" + role +
                ", task='" + task + '\'' +
                ", tags=" + tags +
                ", folder='" + folder + '\'' +
                '}';
    }

}