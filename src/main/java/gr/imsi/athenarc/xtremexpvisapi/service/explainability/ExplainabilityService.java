package gr.imsi.athenarc.xtremexpvisapi.service.explainability;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.springframework.cache.annotation.Cacheable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import explainabilityService.ApplyAffectedActionsRequest;
import explainabilityService.ApplyAffectedActionsResponse;
import explainabilityService.ExplanationsGrpc;
import explainabilityService.ExplanationsGrpc.ExplanationsBlockingStub;
import explainabilityService.ExplanationsGrpc.ExplanationsImplBase;
import explainabilityService.ExplanationsRequest;
import explainabilityService.ExplanationsResponse;
import explainabilityService.FeatureImportanceRequest;
import explainabilityService.FeatureImportanceResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import lombok.extern.java.Log;

@Service
@Log
public class ExplainabilityService extends ExplanationsImplBase {

        private final String grpcHostName;
        private final String grpcHostPort;
        private final ExplainabilityRunHelper explainabilityRunHelper;
        private final ManagedChannel channel;
        private final ExplanationsBlockingStub stub;

        public ExplainabilityService(@Value("${app.grpc.host.name}") String grpcHostName,
                        @Value("${app.grpc.host.port}") String grpcHostPort,
                        ExplainabilityRunHelper explainabilityRunHelper) {
                this.grpcHostName = grpcHostName;
                this.grpcHostPort = grpcHostPort;
                this.explainabilityRunHelper = explainabilityRunHelper;
                this.channel = ManagedChannelBuilder.forAddress(grpcHostName, Integer.parseInt(grpcHostPort))
                .usePlaintext()
                .maxInboundMessageSize(50 * 1024 * 1024)
                .maxInboundMetadataSize(50 * 1024 * 1024)
                // Keep-alive settings - increased to avoid "too many pings" error
                .keepAliveTime(120, TimeUnit.SECONDS)  // Minimum 2 minutes between pings
                .keepAliveTimeout(20, TimeUnit.SECONDS)  // Wait longer for ping ACK
                .keepAliveWithoutCalls(true)  // Keep connection alive even without active calls
                .build();
                
                this.stub = ExplanationsGrpc.newBlockingStub(channel);
        }

        // Helper used by SpEL in @Cacheable key expression
        public static String explainKey(String explainabilityRequest, String experimentId, String runId, String authorization) {
                String raw = (explainabilityRequest == null ? "" : explainabilityRequest)
                                + "|" + (experimentId == null ? "" : experimentId)
                                + "|" + (runId == null ? "" : runId)
                                + "|" + (authorization == null ? "" : authorization);
                return "GetExplains:" + sha256Hex(raw);
        }

        private static String sha256Hex(String input) {
                try {
                        MessageDigest md = MessageDigest.getInstance("SHA-256");
                        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
                        StringBuilder sb = new StringBuilder();
                        for (byte b : digest) sb.append(String.format("%02x", b));
                        return sb.toString();
                } catch (NoSuchAlgorithmException e) {
                        return Integer.toString(input.hashCode());
                }
        }

       @Cacheable(value = "explanations", key = "T(gr.imsi.athenarc.xtremexpvisapi.service.explainability.ExplainabilityService).explainKey(#explainabilityRequest,#experimentId,#runId,#authorization)")
    public JsonNode GetExplains(String explainabilityRequest,
                    String experimentId, String runId, String authorization)
                    throws InvalidProtocolBufferException, JsonProcessingException {

        ExplanationsRequest request = explainabilityRunHelper.requestBuilder(explainabilityRequest,
                        experimentId, runId, authorization);

        // Use the reused stub - no channel creation/shutdown
        ExplanationsResponse response = stub.getExplanation(request);
        
        String jsonString = JsonFormat.printer().print(response);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(jsonString);
    }

    public JsonNode ApplyAffectedActions() throws InvalidProtocolBufferException, JsonProcessingException {
        ApplyAffectedActionsRequest request = ApplyAffectedActionsRequest.newBuilder().build();
        
        // Use the reused stub - no channel creation/shutdown
        ApplyAffectedActionsResponse response = stub.applyAffectedActions(request);
        
        String jsonString = JsonFormat.printer().print(response);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(jsonString);
    }

    @Cacheable(value = "featureImportance", key = "T(gr.imsi.athenarc.xtremexpvisapi.service.explainability.ExplainabilityService).explainKey(#featureImportanceRequest,#experimentId,#runId,#authorization)")
    public JsonNode getFeatureImportance(String featureImportanceRequest, String experimentId, String runId,
                    String authorization) throws InvalidProtocolBufferException, JsonProcessingException {

        FeatureImportanceRequest request = explainabilityRunHelper.featureImportanceRequestBuilder(
                        featureImportanceRequest, experimentId, runId, authorization);

        // Use the reused stub - no channel creation/shutdown
        FeatureImportanceResponse response = stub.getFeatureImportance(request);
        
        String jsonString = JsonFormat.printer().print(response);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(jsonString);
    }

    @PreDestroy
    public void cleanup() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
            try {
                if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                    channel.shutdownNow();
                }
            } catch (InterruptedException e) {
                channel.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
