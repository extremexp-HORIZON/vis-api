// package gr.imsi.athenarc.xtremexpvisapi.service;
// import java.net.URI;
// import java.net.http.HttpClient;
// import java.net.http.HttpRequest;
// import java.net.http.HttpResponse;
// import java.time.Duration;

// import org.springframework.stereotype.Service;

// @Service
// public class ZenohService {
//     private HttpClient httpClient;
//     private final String baseUrl = "http://127.0.0.1:5000";
//     private String accessToken; // Store the token here

//     public ZenohService() {
//         this.httpClient = HttpClient.newBuilder()
//             .version(HttpClient.Version.HTTP_2)
//             .connectTimeout(Duration.ofSeconds(10))
//             .build();
//     }

//     public String authenticate(String username, String password) throws Exception {
//         HttpRequest request = HttpRequest.newBuilder()
//             .uri(URI.create(baseUrl + "/auth/login"))
//             .headers("Content-Type", "application/x-www-form-urlencoded")
//             .POST(HttpRequest.BodyPublishers.ofString("username=" + username + "&password=" + password))
//             .build();
        
//         HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
//         // Parse the token from the response and store it
//         this.accessToken = parseToken(response.body()); // Implement this method to parse token
//         return this.accessToken;
//     }

//     // Use stored token for this request
//     public String listFiles(String useCase, String folder) throws Exception {
//         if (accessToken == null) {
//             throw new IllegalStateException("User is not authenticated");
//         }

//         HttpRequest request = HttpRequest.newBuilder()
//             .uri(URI.create(baseUrl + "/list/" + useCase + "/" + folder))
//             .headers("Authorization", "Bearer " + accessToken, "Accept", "application/json")
//             .GET()
//             .build();

//         HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
//         return response.body();
//     }
// }

// // @Service

// // public class ZenohService {
// //     private HttpClient httpClient;
// //     private final String baseUrl = "http://127.0.0.1:5000";

// //     public ZenohService() {
// //         this.httpClient = HttpClient.newBuilder()
// //             .version(HttpClient.Version.HTTP_2)
// //             .connectTimeout(Duration.ofSeconds(10))
// //             .build();
// //     }

// //     // Method to authenticate and return the access token
// //     public String authenticate(String username, String password) throws Exception {
// //         HttpRequest request = HttpRequest.newBuilder()
// //             .uri(URI.create(baseUrl + "/auth/login"))
// //             .headers("Content-Type", "application/x-www-form-urlencoded")
// //             .POST(HttpRequest.BodyPublishers.ofString("username=" + username + "&password=" + password))
// //             .build();

// //         HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
// //         return response.body();
// //     }

// //     public String listFiles(String token, String useCase, String folder) throws Exception {
// //         HttpRequest request = HttpRequest.newBuilder()
// //             .uri(URI.create(baseUrl + "/list/" + useCase + "/" + folder))
// //             .headers("Authorization", "Bearer " + token, "Accept", "application/json")
// //             .GET()
// //             .build();
// //         HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
// //         return response.body();
// //     }

// //     // Add more methods here to interact with other Zenoh endpoints
// // }












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
    private ObjectMapper objectMapper = new ObjectMapper(); // Jackson object mapper

    public ZenohService() {
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    public String authenticate(String username, String password) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/auth/login"))
            .headers("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("username=" + username + "&password=" + password))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("resbody"+response.body());
        this.accessToken = parseToken(response.body()); // Parse and store the token
        return this.accessToken;
    }

    private String parseToken(String responseBody) throws Exception {
        JsonNode rootNode = objectMapper.readTree(responseBody);
        JsonNode tokensNode = rootNode.path("tokens");
        if (tokensNode.isMissingNode()) {
            throw new IllegalStateException("Token not found in the response");
        }
        return tokensNode.path("access").asText();
    }

    public String listFiles() throws Exception {
        if (accessToken == null) {
            throw new IllegalStateException("User is not authenticated");
        }
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:5000/list/query_files"))
            .header("Authorization", "Bearer " + accessToken)  // Include the access token in the Authorization header
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.noBody())  // Assuming no body is required for this request
            .build();
    
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new Exception("Failed to list files, status code: " + response.statusCode());
        }
    }

    public String CasesFiles(String useCase, String folder) throws Exception {
        if (accessToken == null) {
            throw new IllegalStateException("User is not authenticated");
        }

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/list/" + useCase + "/" + folder))
            .headers("Authorization", "Bearer " + accessToken, "Accept", "application/json")
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }


    public String getFile(String useCase, String folder, String subfolder, String filename) throws Exception {
        if (accessToken == null) {
            throw new IllegalStateException("User is not authenticated");
        }
    
        URI fileUri = URI.create(String.format("%s/file/%s/%s/%s/%s", baseUrl, useCase, folder, subfolder, filename));
        HttpRequest request = HttpRequest.newBuilder()
            .uri(fileUri)
            .header("Authorization", "Bearer " + accessToken)
            .GET()
            .build();
    
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new Exception("Failed to retrieve file, status code: " + response.statusCode());
        }
    }

    

    public String deleteFile(String useCase, String folder, String subfolder, String filename) throws Exception {
        if (accessToken == null) {
            throw new IllegalStateException("User is not authenticated");
        }
    
        URI fileUri = URI.create(String.format("%s/file/%s/%s/%s/%s", baseUrl, useCase, folder, subfolder, filename));
        HttpRequest request = HttpRequest.newBuilder()
            .uri(fileUri)
            .header("Authorization", "Bearer " + accessToken)
            .DELETE()
            .build();
    
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return "File successfully deleted";
        } else {
            throw new Exception("Failed to delete file, status code: " + response.statusCode());
        }
    }
    
}
