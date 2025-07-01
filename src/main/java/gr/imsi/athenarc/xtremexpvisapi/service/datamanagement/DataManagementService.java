package gr.imsi.athenarc.xtremexpvisapi.service.datamanagement;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;

import gr.imsi.athenarc.xtremexpvisapi.domain.DataManagement.CatalogRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.DataManagement.CatalogResponse;
import lombok.extern.java.Log;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Log
public class DataManagementService {

    private HttpClient httpClient;

    @Value("${app.zenoh.baseurl}")
    private String baseUrl;

    private ObjectMapper objectMapper = new ObjectMapper(); // Jackson object mapper

    public DataManagementService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    private HttpRequest.Builder requestBuilder(String path) {
        if (path.contains("http")) {
            return HttpRequest.newBuilder()
                    .uri(URI.create(path));
        }
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/file/" + path));
    }

    private String buildQueryString(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }

        StringBuilder queryString = new StringBuilder("?");
        boolean first = true;

        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                queryString.append("&");
            }
            queryString.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            queryString.append("=");
            queryString.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            first = false;
        }

        return queryString.toString();
    }

    @Async
    @Cacheable(value = "experimentFiles", key = "#catalogRequest.toMap().toString()")
    public CompletableFuture<CatalogResponse> getCatalogInfo(CatalogRequest catalogRequest) throws Exception {
        Map<String, String> params = new HashMap<>();
        HttpRequest request = requestBuilder("/catalog" + buildQueryString(catalogRequest.toMap()))
                .GET()
                .build();

        log.info("Sending request to: " + baseUrl + "/catalog" + buildQueryString(params));

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Error handling
        if (response.statusCode() >= 300) {
            log.severe("Error fetching catalog data: " + response.statusCode() + " - " + response.body());
            throw new RuntimeException("Failed to fetch catalog data: " + response.statusCode());
        }

        try {
            CatalogResponse result = objectMapper.readValue(response.body(), CatalogResponse.class);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.severe("Failed to parse response: " + e.getMessage());
            throw new RuntimeException("Failed to parse response", e);
        }
    }

}