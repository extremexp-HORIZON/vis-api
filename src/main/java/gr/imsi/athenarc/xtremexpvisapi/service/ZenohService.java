package gr.imsi.athenarc.xtremexpvisapi.service;
 
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
 
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
 
@Service
public class ZenohService {
    private HttpClient httpClient;
    private final String baseUrl = "http://127.0.0.1:5000";
    private String accessToken; // Store the token here
    private String refreshToken; // Store the refresh token
 
    private ObjectMapper objectMapper = new ObjectMapper(); // Jackson object mapper
 
    public ZenohService() {
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }
 
    public String authenticate(String username, String password) throws Exception {
        String form = "username=" + username + "&password=" + password;
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
        rootNode.path("access_token").asText();
        // parseAndStoreTokens(response.body()); // Parse and store tokens
        return this.accessToken;
    }
 
    public String CasesFiles(String useCase, String folder, String subfolder, String filename) throws Exception {
        if (accessToken == null) {
            authenticate("admin", "adminxp");
        }
 
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/file/" + useCase + "/" + folder + "/" + subfolder + "/" + filename))
            .headers("Authorization", "Bearer " + accessToken, "Accept", "application/json")
            .GET()
            .build();
 
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 401) {
            refresh(); // Refresh the token
            request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/file/" + useCase + "/" + folder + "/" + subfolder + "/" + filename))
                .headers("Authorization", "Bearer " + accessToken, "Accept", "application/json")
                .GET()
                .build();
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        }
        return response.body();
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