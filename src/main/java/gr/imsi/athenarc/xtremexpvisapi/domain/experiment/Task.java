package gr.imsi.athenarc.xtremexpvisapi.domain.experiment;

import java.util.Map;

/**
 * Represents a single task within a run.
 * A run may consist of multiple tasks, each corresponding to a step in the
 * execution workflow.
 * This task-based organization is only applicable if supported by the
 * experiment tracking tool.
 * For example, MLflow does not natively track a task structure.
 */
public class Task {

    /**
     * The name of the task.
     */
    private String name;

    /**
     * The type of the task
     */
    private String type;

    /**
     * The specific implementation of the task used in this run (e.g., `"TrainRNN"`,
     * `"TrainNN"`)
     */
    private String variant;

    /**
     * Unix timestamp (milliseconds) when the task started.
     * This field is optional.
     */
    private Long startTime;

    /**
     * Unix timestamp (milliseconds) when the task ended.
     * This field is optional.
     */
    private Long endTime;

    /**
     * Additional metadata associated with the task, stored as key-value pairs.
     * This field is optional.
     */
    private Map<String, String> tags;

    private String id;

    // Constructors

    public Task() {
    }

    public Task(String name, String type, String variant, Long startTime, Long endTime, Map<String, String> tags ,String id) {
        this.name = name;
        this.type = type;
        this.variant = variant;
        this.startTime = startTime;
        this.endTime = endTime;
        this.tags = tags;
        this.id = id;
    }

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
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

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    @Override
    public String toString() {
        return "Task{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", variant='" + variant + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", tags=" + tags +
                ", id='" + id + '\'' +
                '}';
    }

}
