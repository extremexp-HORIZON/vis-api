package gr.imsi.athenarc.xtremexpvisapi.service.observability;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class ObservabilityServiceFactory {

    private final Map<String, ObservabilityService> observabilityServices;
    private final String configuredService;

    public ObservabilityServiceFactory(Map<String, ObservabilityService> observabilityServices,
                                 @Value("${observability.service:langfuse}") String configuredService) {
        this.observabilityServices = observabilityServices;
        this.configuredService = configuredService;
    }

    public ObservabilityService getObservabilityService() {
        ObservabilityService service = observabilityServices.get(configuredService);
        if (service == null) {
            throw new IllegalArgumentException("Observability service not found: " + configuredService + 
                    ". Available services: " + observabilityServices.keySet());
        }
        return service;
    }

    public ObservabilityService getObservabilityService(String serviceName) {
        ObservabilityService service = observabilityServices.get(serviceName);
        if (service == null) {
            throw new IllegalArgumentException("Observability service not found: " + serviceName + 
                    ". Available services: " + observabilityServices.keySet());
        }
        return service;
    }
}
