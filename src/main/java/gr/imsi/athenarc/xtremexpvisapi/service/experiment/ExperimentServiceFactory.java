package gr.imsi.athenarc.xtremexpvisapi.service.experiment;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Factory that provides the configured experiment service implementation. */
@Component
public class ExperimentServiceFactory {

  private final Map<String, ExperimentService> experimentServices;
  private final String activeEngine;

  @Autowired
  public ExperimentServiceFactory(
      Map<String, ExperimentService> experimentServices,
      @Value("${experiment.engine:extremeXP}") String activeEngine) {
    this.experimentServices = experimentServices;
    this.activeEngine = activeEngine;
  }

  /**
   * Returns the active experiment service based on configuration.
   *
   * @return The active ExperimentService implementation
   */
  public ExperimentService getActiveService() {
    ExperimentService service = experimentServices.get(activeEngine);
    if (service == null) {
      throw new IllegalStateException("No experiment service found for engine: " + activeEngine);
    }
    return service;
  }
}
