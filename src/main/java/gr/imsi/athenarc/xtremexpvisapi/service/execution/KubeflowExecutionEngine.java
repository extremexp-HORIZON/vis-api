package gr.imsi.athenarc.xtremexpvisapi.service.execution;

import gr.imsi.athenarc.xtremexpvisapi.domain.kubeflow.KfpPipelineResponse;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

/**
 * Kubeflow Pipelines implementation of ExecutionEngine. Integrates with Kubeflow Pipelines API for
 * pipeline execution.
 */
@Component("kubeflow")
public class KubeflowExecutionEngine implements ExecutionEngine {

  private final RestTemplate restTemplate;
  private final String baseUrl;

  public KubeflowExecutionEngine(
      RestTemplate restTemplate, @Value("${kubeflow.pipelines.base-url}") String baseUrl) {
    this.restTemplate = restTemplate;
    this.baseUrl = baseUrl;
  }

  @Override
  public String findPipelineIdByName(String pipelineName) throws ExecutionEngineException {
    String url = baseUrl + "/apis/v1beta1/pipelines?page_size=200";

    ResponseEntity<KfpPipelineResponse> resp;
    try {
      resp = restTemplate.exchange(url, HttpMethod.GET, null, KfpPipelineResponse.class);
    } catch (RestClientResponseException e) {
      throw new ExecutionEngineException(
          "Kubeflow: Failed to list pipelines ("
              + e.getStatusCode()
              + "): "
              + e.getResponseBodyAsString(),
          e);
    }

    KfpPipelineResponse body = resp.getBody();
    if (body == null || body.pipelines == null) return null;

    return body.pipelines.stream()
        .filter(p -> pipelineName.equals(p.name))
        .map(p -> p.id)
        .findFirst()
        .orElse(null);
  }

  @Override
  public String createRun(String pipelineId, String runName, Map<String, String> params)
      throws ExecutionEngineException {

    String url = baseUrl + "/apis/v1beta1/runs";

    Map<String, Object> typedParams = new HashMap<>();
    if (params != null) {
      for (Map.Entry<String, String> e : params.entrySet()) {
        typedParams.put(e.getKey(), coerceValue(e.getValue()));
      }
    }

    Map<String, Object> runtimeConfig = new HashMap<>();
    runtimeConfig.put("parameters", typedParams);

    Map<String, Object> pipelineSpec = new HashMap<>();
    pipelineSpec.put("pipeline_id", pipelineId);
    pipelineSpec.put("runtime_config", runtimeConfig);

    Map<String, Object> body = new HashMap<>();
    body.put("name", runName);
    body.put("pipeline_spec", pipelineSpec);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);

    ResponseEntity<Map> resp;
    try {
      resp = restTemplate.exchange(url, HttpMethod.POST, req, Map.class);
    } catch (RestClientResponseException e) {
      throw new ExecutionEngineException(
          "Kubeflow: Failed to create run ("
              + e.getStatusCode()
              + "): "
              + e.getResponseBodyAsString(),
          e);
    }

    Map respBody = resp.getBody();
    if (respBody == null) return null;

    Map run = (Map) respBody.get("run");
    if (run == null) return null;

    return (String) run.get("id");
  }

  @Override
  public void terminateRun(String runId) throws ExecutionEngineException {
    String url = baseUrl + "/apis/v1beta1/runs/" + runId + "/terminate";
    try {
      restTemplate.exchange(
          url, HttpMethod.POST, new HttpEntity<>(new HttpHeaders()), String.class);
    } catch (RestClientResponseException e) {
      throw new ExecutionEngineException(
          "Kubeflow: Failed to terminate run ("
              + e.getStatusCode()
              + "): "
              + e.getResponseBodyAsString(),
          e);
    }
  }

  /** Coerce string values to appropriate types (boolean, int, double, or String). */
  private Object coerceValue(String value) {
    if (value == null) return null;

    String v = value.trim();

    if (v.equalsIgnoreCase("true")) return true;
    if (v.equalsIgnoreCase("false")) return false;

    try {
      if (!v.contains(".")) {
        return Integer.parseInt(v);
      }
    } catch (NumberFormatException ignored) {
    }

    try {
      return Double.parseDouble(v);
    } catch (NumberFormatException ignored) {
    }

    return v;
  }
}
