package gr.imsi.athenarc.xtremexpvisapi.service.observability;

import org.springframework.stereotype.Service;

import gr.imsi.athenarc.xtremexpvisapi.domain.observability.TracesResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.observability.TraceDetail;

public interface ObservabilityService {
    TracesResponse getTraces(String projectId, String sessionId, String userId);
    TraceDetail getTrace(String traceId);
}
