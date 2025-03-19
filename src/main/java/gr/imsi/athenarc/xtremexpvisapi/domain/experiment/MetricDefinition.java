package gr.imsi.athenarc.xtremexpvisapi.domain.experiment;

/**
 * Represents metadata about a metric tracked within an experiment.
 */
public class MetricDefinition {

    /**
     * The metric name (e.g., "accuracy", "loss").
     */
    private String name;

    /**
     * A brief explanation of what the metric represents.
     * This field is optional.
     */
    private String description;

    /**
     * A standardized metric type (e.g., "accuracy", "precision", "f1-score").
     * This field is optional.
     */
    private String semanticType;

    /**
     * The unit of measurement (e.g., percentage, seconds).
     * This field is optional.
     */
    private String unit;

    /**
     * Whether a higher value is considered better.
     * This field is optional.
     */
    private Boolean greaterIsBetter;

    // Constructors

    public MetricDefinition() {
    }

    public MetricDefinition(String name, String description, String semanticType, String unit, Boolean greaterIsBetter) {
        this.name = name;
        this.description = description;
        this.semanticType = semanticType;
        this.unit = unit;
        this.greaterIsBetter = greaterIsBetter;
    }

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSemanticType() {
        return semanticType;
    }

    public void setSemanticType(String semanticType) {
        this.semanticType = semanticType;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Boolean getGreaterIsBetter() {
        return greaterIsBetter;
    }

    public void setGreaterIsBetter(Boolean greaterIsBetter) {
        this.greaterIsBetter = greaterIsBetter;
    }

    // toString() Method for Debugging

    @Override
    public String toString() {
        return "MetricDefinition{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", semanticType='" + semanticType + '\'' +
                ", unit='" + unit + '\'' +
                ", greaterIsBetter=" + greaterIsBetter +
                '}';
    }
}
