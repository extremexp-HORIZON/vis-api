package gr.imsi.athenarc.xtremexpvisapi.domain.experiment;

import java.util.List;
import java.util.Map;

/**
 * Represents an experiment, which serves as a top-level container for multiple
 * runs.
 */
public class Experiment {

    /**
     * Unique identifier for the experiment.
     */
    private String id;

    /**
     * A human-readable name that identifies the experiment.
     * This field is optional.
     */
    private String name;

    /**
     * Unix timestamp of when the experiment was created (in milliseconds).
     */
    private Long creationTime;

    /**
     * Unix timestamp of the last modification of the experiment (in milliseconds).
     * This field is optional.
     */
    private Long lastUpdateTime;

    /**
     * Definitions of metrics tracked within this experiment.
     * This field is optional.
     */
    private List<MetricDefinition> metricDefinitions;

    /**
     * Additional metadata for the experiment, stored as key-value pairs.
     */
    private Map<String, String> tags;

    
    // Constructors

    public Experiment() {
    }

    public Experiment(String id, String name, Long creationTime, Long lastUpdateTime,
            List<MetricDefinition> metricDefinitions, Map<String, String> tags) {
        this.id = id;
        this.name = name;
        this.creationTime = creationTime;
        this.lastUpdateTime = lastUpdateTime;
        this.metricDefinitions = metricDefinitions;
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

    public Long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Long creationTime) {
        this.creationTime = creationTime;
    }

    public Long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(Long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public List<MetricDefinition> getMetricDefinitions() {
        return metricDefinitions;
    }

    public void setMetricDefinitions(List<MetricDefinition> metricDefinitions) {
        this.metricDefinitions = metricDefinitions;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    // toString() Method for Debugging

    @Override
    public String toString() {
        return "Experiment{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", creationTime=" + creationTime +
                ", lastUpdateTime=" + lastUpdateTime +
                ", metricDefinitions=" + metricDefinitions +
                ", tags=" + tags +
                '}';
    }
}
