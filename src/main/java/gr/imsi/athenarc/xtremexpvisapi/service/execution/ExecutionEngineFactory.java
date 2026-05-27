package gr.imsi.athenarc.xtremexpvisapi.service.execution;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Factory for selecting the appropriate execution engine based on configuration. Supports pluggable
 * execution engines (Kubeflow, Airflow, etc.)
 */
@Component
public class ExecutionEngineFactory {

  private final Map<String, ExecutionEngine> executionEngines;
  private final String configuredEngine;

  public ExecutionEngineFactory(
      Map<String, ExecutionEngine> executionEngines,
      @Value("${mlflow.execution.engine:kubeflow}") String configuredEngine) {
    this.executionEngines = executionEngines;
    this.configuredEngine = configuredEngine;
  }

  /**
   * Get the configured execution engine.
   *
   * @return The execution engine instance
   * @throws IllegalArgumentException if the configured engine is not available
   */
  public ExecutionEngine getExecutionEngine() {
    ExecutionEngine engine = executionEngines.get(configuredEngine);
    if (engine == null) {
      throw new IllegalArgumentException(
          "Execution engine not found: "
              + configuredEngine
              + ". Available engines: "
              + executionEngines.keySet());
    }
    return engine;
  }

  /**
   * Get a specific execution engine by name.
   *
   * @param engineName The name of the engine to retrieve
   * @return The execution engine instance
   * @throws IllegalArgumentException if the engine is not available
   */
  public ExecutionEngine getExecutionEngine(String engineName) {
    ExecutionEngine engine = executionEngines.get(engineName);
    if (engine == null) {
      throw new IllegalArgumentException(
          "Execution engine not found: "
              + engineName
              + ". Available engines: "
              + executionEngines.keySet());
    }
    return engine;
  }
}
