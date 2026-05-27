package gr.imsi.athenarc.xtremexpvisapi.service.execution;

/** Exception thrown by execution engines when operations fail. */
public class ExecutionEngineException extends Exception {

  public ExecutionEngineException(String message) {
    super(message);
  }

  public ExecutionEngineException(String message, Throwable cause) {
    super(message, cause);
  }
}
