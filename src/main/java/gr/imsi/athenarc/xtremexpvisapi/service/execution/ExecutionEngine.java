package gr.imsi.athenarc.xtremexpvisapi.service.execution;

import java.util.Map;

/**
 * Abstraction for execution engines (Kubeflow, Airflow, etc.)
 * Defines the contract for pipeline execution operations.
 */
public interface ExecutionEngine {

    /**
     * Find a pipeline by name.
     *
     * @param pipelineName The name of the pipeline to find
     * @return The pipeline ID, or null if not found
     * @throws ExecutionEngineException if the operation fails
     */
    String findPipelineIdByName(String pipelineName) throws ExecutionEngineException;

    /**
     * Create and run a pipeline with the given parameters.
     *
     * @param pipelineId The ID of the pipeline to run
     * @param runName The name for this run
     * @param params A map of parameter names to values (as strings)
     * @return The run ID created by the execution engine
     * @throws ExecutionEngineException if the operation fails
     */
    String createRun(String pipelineId, String runName, Map<String, String> params) throws ExecutionEngineException;

    /**
     * Terminate a running pipeline.
     *
     * @param runId The ID of the run to terminate
     * @throws ExecutionEngineException if the operation fails
     */
    void terminateRun(String runId) throws ExecutionEngineException;

}
