package gr.imsi.athenarc.xtremexpvisapi.domain.experiment;

import java.util.List;
import java.util.Map;

/**
 * Represents a single run within an experiment (i.e., a workflow in ExtremeXP
 * terminology),
 * encapsulating metadata, parameters (i.e., variability points in ExtremeXP),
 * metrics,
 * data assets, and optionally, tasks.
 * If multiple values are logged for the same metric key within a run (e.g.,
 * across epochs),
 * only the value with the latest timestamp is returned.
 */
public class Run {

    /**
     * Unique identifier for the run.
     */
    private String id;

    /**
     * The name of the run. This field is optional.
     */
    private String name;

    /**
     * The experiment this run belongs to.
     */
    private String experimentId;

    /**
     * Enum representing the possible statuses of a Run.
     */
    public enum Status {
        RUNNING, SCHEDULED, COMPLETED, FAILED, PAUSED, STOPPED, KILLED
    }

    /**
     * The status of the run. One of: RUNNING, SCHEDULED, COMPLETED, FAILED, PAUSED,
     * STOPPED, KILLED.
     */
    private Status status;

    /**
     * Unix timestamp of when the run started (in milliseconds). This field is
     * optional.
     */
    private Long startTime;

    /**
     * Unix timestamp of when the run ended (in milliseconds). This field is
     * optional.
     */
    private Long endTime;

    /**
     * Parameters used for the run.
     */
    private List<Param> params;

    /**
     * Metrics collected during the run.
     */
    private List<Metric> metrics;

    /**
     * Input datasets and output artifacts associated with the run.
     */
    private List<DataAsset> dataAssets;

    /**
     * A list of task names associated with the run.
     * This field is optional.
     */
    private List<Task> tasks;

    /**
     * Additional metadata for the run, stored as key-value pairs.
     */
    private Map<String, String> tags;

    // Constructors

    public Run() {
    }

    public Run(String id, String name, String experimentId, Status status, Long startTime, Long endTime,
            List<Param> params, List<Metric> metrics, List<DataAsset> dataAssets,
            List<Task> tasks, Map<String, String> tags) {
        this.id = id;
        this.name = name;
        this.experimentId = experimentId;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.params = params;
        this.metrics = metrics;
        this.dataAssets = dataAssets;
        this.tasks = tasks;
        this.tags = tags;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public List<Param> getParams() {
        return params;
    }

    public void setParams(List<Param> params) {
        this.params = params;
    }

    public List<Metric> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<Metric> metrics) {
        this.metrics = metrics;
    }

    public List<DataAsset> getDataAssets() {
        return dataAssets;
    }

    public void setDataAssets(List<DataAsset> dataAssets) {
        this.dataAssets = dataAssets;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    @Override
    public String toString() {
        return "Run{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", experimentId='" + experimentId + '\'' +
                ", status=" + status +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", params=" + params +
                ", metrics=" + metrics +
                ", dataAssets=" + dataAssets +
                ", tasks=" + tasks +
                ", tags=" + tags +
                '}';
    }
}