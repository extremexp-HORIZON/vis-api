package gr.imsi.athenarc.xtremexpvisapi.domain.experiment;

import java.util.Map;

/**
 * Represents an input dataset or output artifact used in an experiment.
 */
public class DataAsset {

    /**
     * Enum representing the possible roles of a DataAsset.
     * Defines whether the asset is an input dataset or an output artifact.
     */
    public enum Role {
        INPUT, // Represents input datasets
        OUTPUT // Represents output artifacts
    }

    /**
     * The name of the data asset.
     */
    private String name;

    /**
     * The type of the data source (e.g., "http", "local").
     */
    private String sourceType;

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
     * Additional metadata related to the data asset, stored as key-value pairs.
     * This field is optional.
     */
    private Map<String, String> tags;

    // Constructors

    public DataAsset() {
    }

    public DataAsset(String name, String sourceType, String source, String format,
            Role role, String task, Map<String, String> tags) {
        this.name = name;
        this.sourceType = sourceType;
        this.source = source;
        this.format = format;
        this.role = role;
        this.task = task;
        this.tags = tags;
    }

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
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
                '}';
    }
}