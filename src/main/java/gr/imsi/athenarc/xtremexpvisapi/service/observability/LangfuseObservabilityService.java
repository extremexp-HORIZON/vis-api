package gr.imsi.athenarc.xtremexpvisapi.service.observability;

import gr.imsi.athenarc.xtremexpvisapi.domain.observability.TracesResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service("langfuse")
public class LangfuseObservabilityService implements ObservabilityService {

    private final RestTemplate restTemplate;
    private final String apiUrl;

    public LangfuseObservabilityService(RestTemplateBuilder restTemplateBuilder,
                                        @Value("${langfuse.api.url}") String apiUrl,
                                        @Value("${langfuse.api.publicKey}") String publicKey,
                                        @Value("${langfuse.api.secretKey}") String secretKey) {
        this.apiUrl = apiUrl;
        this.restTemplate = restTemplateBuilder
                .basicAuthentication(publicKey, secretKey)
                .build();
    }

    @Override
    public TracesResponse getTraces(String projectId, String sessionId) {
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .path("/api/public/traces")
                .queryParam("projectId", projectId)
                .queryParam("fields", "observations,scores");

        if (sessionId != null && !sessionId.isEmpty()) {
            uriBuilder.queryParam("sessionId", sessionId);
        }

        ResponseEntity<TracesResponse> response = restTemplate.exchange(
                uriBuilder.toUriString(),
                HttpMethod.GET,
                entity,
                TracesResponse.class
        );

        return response.getBody();
    }
}
