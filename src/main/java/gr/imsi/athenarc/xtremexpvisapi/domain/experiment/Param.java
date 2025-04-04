package gr.imsi.athenarc.xtremexpvisapi.domain.experiment;

/**
 * Represents a key-value pair used as an input parameter for a run.
 * Parameters typically define the configuration of an experiment, such as
 * hyperparameters for training models.
 */
public class Param {

    /**
     * The name of the parameter (e.g., "learning_rate", "batch_size").
     */
    private String name;

    /**
     * The value assigned to the parameter (e.g., "0.001", "32").
     */
    private String value;

    /**
     * The task this parameter is associated with, if applicable.
     */
    private String task;

    // Constructors

    public Param() {
    }

    public Param(String name, String value, String task) {
        this.name = name;
        this.value = value;
        this.task = task;
    }

    // Add this missing constructor:
    public Param(String name, String value) {
        this.name = name;
        this.value = value;
        this.task = "default"; // Or any appropriate default value
    }

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    @Override
    public String toString() {
        return "Param{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", task='" + task + '\'' +
                '}';
    }

}
