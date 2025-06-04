package gr.imsi.athenarc.xtremexpvisapi.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.xml.catalog.Catalog;

import com.fasterxml.jackson.databind.ObjectMapper;

import gr.imsi.athenarc.xtremexpvisapi.domain.DataManagement.CatalogRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.DataManagement.CatalogResponse;
import lombok.extern.java.Log;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

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

    private ObjectMapper objectMapper = new ObjectMapper(); // Jackson object mapper

    public ZenohService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    private HttpRequest.Builder requestBuilder(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path));
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

    // public String authenticate() throws Exception {
    // String form = "username=" + username + "&password=" + password;
    // log.info("username: " + username);
    // HttpRequest request = HttpRequest.newBuilder()
    // .uri(URI.create(baseUrl + "/auth/login"))
    // .header("Content-Type", "application/x-www-form-urlencoded")
    // .POST(HttpRequest.BodyPublishers.ofString(form))
    // .build();

    // HttpResponse<String> response = httpClient.send(request,
    // HttpResponse.BodyHandlers.ofString());
    // System.out.println("resbody: " + response.body());
    // if (response.statusCode() != 200) {
    // throw new IllegalStateException("Failed to authenticate: " +
    // response.body());
    // }
    // parseAndStoreTokens(response.body()); // Parse and store tokens
    // return this.accessToken;
    // }

    // public String refresh() throws Exception {
    // HttpRequest request = HttpRequest.newBuilder()
    // .uri(URI.create(baseUrl + "/auth/refresh"))
    // .headers("Authorization", "Bearer " + refreshToken, "Content-Type",
    // "application/json")
    // .POST(HttpRequest.BodyPublishers.noBody())
    // .build();

    // HttpResponse<String> response = httpClient.send(request,
    // HttpResponse.BodyHandlers.ofString());
    // JsonNode rootNode = objectMapper.readTree(response.body());
    // this.accessToken = rootNode.path("access_token").asText();
    // return this.accessToken;
    // }

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

    /**
     * Downloads a single file from the Zenoh catalog.
     *
     * @param filePath The file path to download.
     * @return A CompletableFuture containing the CatalogResponse with the file
     *         data.
     * @throws Exception If an error occurs during the download.
     */
    // @Async
    // public Path downloadSingleFile(String filePath) throws Exception {

    //     String directoryPath = filePath.substring(0, filePath.lastIndexOf("/") + 1);
    //     String filenameWithExt = filePath.substring(filePath.lastIndexOf("/") + 1);
    //     String filenameNoExt = filenameWithExt.substring(0, filenameWithExt.lastIndexOf("."));

    //     CatalogRequest catalogRequest = new CatalogRequest(List.of(directoryPath), filenameNoExt);

    //     getCatalogInfo(catalogRequest).<Path>thenApply(catalogResponse -> {

    //         List<CatalogResponse.DataItem> files = catalogResponse.getData();
    //         if (files.isEmpty()) {
    //             throw new RuntimeException("No files found for the given path: " + filePath);
    //         } else if (files.size() > 1) {
    //             throw new RuntimeException("More that 1 files with the same name in the path : " + directoryPath);
    //         }
    //         String fileId = files.get(0).getId();

    //         try {
    //             HttpRequest request = requestBuilder("/file/" + fileId)
    //                     .GET()
    //                     .build();

    //             HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    //             if (response.statusCode() >= 300) {
    //                 log.severe("Error fetching catalog data: " + response.statusCode() + " - " + response.body());
    //                 throw new RuntimeException("Failed to fetch catalog data: " + response.statusCode());
    //             }

    //         } catch (HttpServerErrorException | HttpClientErrorException e) {
    //             throw new RuntimeException("Error downloading file: " + filePath + " - " + e.getMessage());
    //         }
    //     }).exceptionally(e -> {
    //         log.severe("Error retrieving catalog data for file: " + filePath);
    //         throw new RuntimeException("Error retrieving catalog data for file: " + filePath);
    //     });
    // }
}