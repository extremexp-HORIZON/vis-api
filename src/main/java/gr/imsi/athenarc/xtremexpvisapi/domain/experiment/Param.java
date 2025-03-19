package gr.imsi.athenarc.xtremexpvisapi.domain.experiment;

/**
 * Represents a key-value pair used as an input parameter for a run.
 * Parameters typically define the configuration of an experiment, such as
 * hyperparameters for training models.
 * They can also represent categorical values, such as the algorithm type (e.g.,
 * "NeuralNetwork", "RecurrentNeuralNetwork").
 * Additionally, they can be used to track task-based variations in
 * workflow-based systems (e.g., ExtremeXP).
 */
public class Param {

    /**
     * The name of the parameter (e.g., "learning_rate", "batch_size").
     */
    private String name;

    /**
     * The value assigned to the parameter (e.g., "0.001", "32", "RNN").
     */
    private String value;

    // Constructors

    public Param() {
    }

    public Param(String name, String value) {
        this.name = name;
        this.value = value;
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

    // toString() Method for Debugging

    @Override
    public String toString() {
        return "Param{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
