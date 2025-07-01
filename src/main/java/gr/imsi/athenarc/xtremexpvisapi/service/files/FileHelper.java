package gr.imsi.athenarc.xtremexpvisapi.service.files;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.DataSource;
import lombok.extern.java.Log;

@Component
@Log
public class FileHelper {

    @Value("${app.zenoh.baseurl}")
    private String zenohBaseUrl;

    private final HttpClient httpClient;

    @Autowired
    public FileHelper() {
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
                .uri(URI.create(zenohBaseUrl + "/file/" + path));
    }

    @Async
    public CompletableFuture<InputStream> downloadFromHTTP(DataSource dataSource, String authorization)
            throws Exception {
        if (authorization == null || authorization.isEmpty()) {
            log.severe("Authorization header is missing or empty");
            throw new IllegalArgumentException("Authorization header is required");
        }
        HttpRequest request = requestBuilder(dataSource.getSource())
                .GET()
                .header("Authorization", authorization)
                .build();

        log.info("Downloading file from: " + request.uri().toString());

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        // Error handling
        if (response.statusCode() >= 300) {
            log.severe("Error downloading file: " + response.statusCode());
            throw new RuntimeException("Failed to download file: " +
                    response.statusCode());
        }

        return CompletableFuture.completedFuture(response.body());
    }
}
