package gr.imsi.athenarc.xtremexpvisapi.domain.experiment;

/**
 * Represents a recorded metric value for a run, typically used to track performance indicators
 * such as accuracy, loss, or execution time.
 */
public class Metric {

    /**
     * The name of the metric (e.g., "accuracy", "loss").
     */
    private String name;

    /**
     * The recorded metric value.
     */
    private double value;

    /**
     * Unix timestamp (in milliseconds) when the metric was logged.
     */
    private long timestamp;

    /**
     * The step index associated with the metric (e.g., epoch number).
     * This field is optional.
     */
    private Integer step;

    /**
     * The task which the metric was produced by.
     * This field is optional.
     */
    private String task;



    // Constructors

    public Metric() {
    }

    public Metric(String name, double value, long timestamp, Integer step, String task) {
        this.name = name;
        this.value = value;
        this.timestamp = timestamp;
        this.step = step;
        this.task = task;
    }

    // Getters and Setters

   
    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getStep() {
        return step;
    }

    public void setStep(Integer step) {
        this.step = step;
    }

    // toString() Method for Debugging

    @Override
    public String toString() {
        return "Metric{" +
                "name='" + name + '\'' +
                ", value=" + value +
                ", timestamp=" + timestamp +
                ", step=" + step +
                ", task='" + task + '\'' +
                '}';
    }
}
