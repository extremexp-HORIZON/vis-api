package gr.imsi.athenarc.xtremexpvisapi.service;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.java.Log;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Log
public class ZenohService {
    private HttpClient httpClient;
    @Value("${app.zenoh.baseurl}")
    private String baseUrl;
    @Value("${app.zenoh.username}")
    private String username;
    @Value("${app.zenoh.password}")
    private String password;
    private String accessToken; // Store the token here
    private String refreshToken;

    private ObjectMapper objectMapper = new ObjectMapper(); // Jackson object mapper

    public ZenohService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String authenticate() throws Exception {
        String form = "username=" + username + "&password=" + password;
        log.info("username: " + username);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/auth/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("resbody: " + response.body());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Failed to authenticate: " + response.body());
        }
        parseAndStoreTokens(response.body()); // Parse and store tokens
        return this.accessToken;
    }

    public String refresh() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/auth/refresh"))
                .headers("Authorization", "Bearer " + refreshToken, "Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode rootNode = objectMapper.readTree(response.body());
        this.accessToken = rootNode.path("access_token").asText();
        return this.accessToken;
    }

    public HttpResponse<InputStream> getSingleFile(String useCase, String folder, String subfolder, String filename) throws Exception {
        if (accessToken == null) {
            authenticate(); // Authenticate if token is not available
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/file/" + useCase + "/" + folder + "/" + subfolder
                        + "/" + filename))
                .headers("Authorization", "Bearer " + accessToken, "Accept", "application/octet-stream")
                .GET()
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() == 401) {
            authenticate(); // If token has expired, refresh it
            request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/file/" + useCase + "/" + folder + "/"
                            + subfolder + "/" + filename))
                    .headers("Authorization", "Bearer " + accessToken, "Accept", "application/octet-stream")
                    .GET()
                    .build();
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        }
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Failed to retrieve file: HTTP status " + response.statusCode());
        }
        return response;
    }

    private void parseAndStoreTokens(String responseBody) throws Exception {
        JsonNode rootNode = objectMapper.readTree(responseBody);
        JsonNode tokensNode = rootNode.path("tokens");
        if (tokensNode.isMissingNode()) {
            throw new IllegalStateException("Token not found in the response");
        }
        this.accessToken = tokensNode.path("access").asText();
        this.refreshToken = tokensNode.path("refresh").asText();
    }
}