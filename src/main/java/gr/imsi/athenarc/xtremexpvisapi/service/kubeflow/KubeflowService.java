package gr.imsi.athenarc.xtremexpvisapi.service.kubeflow;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import gr.imsi.athenarc.xtremexpvisapi.domain.kubeflow.KfpPipelineResponse;

import java.util.*;

@Component
public class KubeflowService {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public KubeflowService(RestTemplate restTemplate,
                              @Value("${kubeflow.pipelines.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public void terminateRun(String kfpRunId) {
        String url = baseUrl + "/apis/v1beta1/runs/" + kfpRunId + "/terminate";
        try {
            restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(new HttpHeaders()), String.class);
        } catch (RestClientResponseException e) {
            throw new RuntimeException("KFP terminate failed (" + e.getStatusCode() + "): " + e.getResponseBodyAsString(), e);
        }
    }

    public String findPipelineIdByName(String pipelineName) {
        String url = baseUrl + "/apis/v1beta1/pipelines?page_size=200";

        ResponseEntity<KfpPipelineResponse> resp;
        try {
            resp = restTemplate.exchange(url, HttpMethod.GET, null, KfpPipelineResponse.class);
        } catch (RestClientResponseException e) {
            throw new RuntimeException("KFP list pipelines failed (" + e.getStatusCode() + "): " + e.getResponseBodyAsString(), e);
        }

        KfpPipelineResponse body = resp.getBody();
        if (body == null || body.pipelines == null) return null;

        return body.pipelines.stream()
                .filter(p -> pipelineName.equals(p.name))
                .map(p -> p.id)
                .findFirst()
                .orElse(null);
    }

    public String createRun(String pipelineId,
                            String runName,
                            Map<String, String> params) {

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
            throw new RuntimeException(
                    "KFP create run failed (" + e.getStatusCode() + "): " + e.getResponseBodyAsString(), e);
        }

        Map respBody = resp.getBody();
        if (respBody == null) return null;

        Map run = (Map) respBody.get("run");
        if (run == null) return null;

        return (String) run.get("id");
    }

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