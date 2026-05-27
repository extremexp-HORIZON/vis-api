package gr.imsi.athenarc.xtremexpvisapi.service.observability;

import gr.imsi.athenarc.xtremexpvisapi.domain.observability.TraceDetail;
import gr.imsi.athenarc.xtremexpvisapi.domain.observability.TracesResponse;

public interface ObservabilityService {
  TracesResponse getTraces(String projectId, String sessionId, String userId);

  TraceDetail getTrace(String traceId);
}
