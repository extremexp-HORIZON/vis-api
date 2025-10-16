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
import lombok.extern.java.Log;

@Service
@Log
public class ExplainabilityService extends ExplanationsImplBase {

        private final String grpcHostName;
        private final String grpcHostPort;
        private final ExplainabilityRunHelper explainabilityRunHelper;

        public ExplainabilityService(@Value("${app.grpc.host.name}") String grpcHostName,
                        @Value("${app.grpc.host.port}") String grpcHostPort,
                        ExplainabilityRunHelper explainabilityRunHelper) {
                this.grpcHostName = grpcHostName;
                this.grpcHostPort = grpcHostPort;
                this.explainabilityRunHelper = explainabilityRunHelper;
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

                // cache key is built from the method arguments; Spring will cache method result
                // using the cache name 'explanations' configured in CacheConfig

                ExplanationsRequest request = explainabilityRunHelper.requestBuilder(explainabilityRequest,
                                experimentId, runId, authorization);
                String jsonString;
                ObjectMapper objectMapper = new ObjectMapper();
                // print the request as JSON
                jsonString = JsonFormat.printer().print(request);
                // log.info("Request: \n" + jsonString);

                ManagedChannel channel = ManagedChannelBuilder.forAddress(grpcHostName,
                                Integer.parseInt(grpcHostPort))
                                .usePlaintext()
                                .maxInboundMessageSize(50 * 1024 * 1024)  // allow responses up to 50 MB
                                .maxInboundMetadataSize(50 * 1024 * 1024) // optional, for large headers
                                .build();

                ExplanationsBlockingStub stub = ExplanationsGrpc.newBlockingStub(channel);

                ExplanationsResponse response = stub.getExplanation(request);
                jsonString = JsonFormat.printer().print(response);
                // log.info("Response: \n" + jsonString);

                channel.shutdown();

                return objectMapper.readTree(jsonString);
        }

        public JsonNode ApplyAffectedActions()
                        throws InvalidProtocolBufferException, JsonProcessingException {
                ApplyAffectedActionsRequest request = ApplyAffectedActionsRequest.newBuilder()
                                .build();

                ManagedChannel channel = ManagedChannelBuilder.forAddress(grpcHostName, Integer.parseInt(grpcHostPort))
                                .usePlaintext()
                                .maxInboundMessageSize(50 * 1024 * 1024)  // allow responses up to 50 MB
                                .maxInboundMetadataSize(50 * 1024 * 1024) // optional, for large headers
                                .build();

                ExplanationsBlockingStub stub = ExplanationsGrpc.newBlockingStub(channel);

                ApplyAffectedActionsResponse response = stub.applyAffectedActions(request);

                // Shutdown the channel
                channel.shutdown();

                String jsonString = JsonFormat.printer().print(response);

                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readTree(jsonString);
        }

        public JsonNode getFeatureImportance(String featureImportanceRequest, String experimentId, String runId,
                        String authorization)
                        throws InvalidProtocolBufferException, JsonProcessingException {

                FeatureImportanceRequest request = explainabilityRunHelper.featureImportanceRequestBuilder(
                                featureImportanceRequest, experimentId, runId, authorization);
                // System.out.println("FeatureImportanceRequestRRRRn: " + request);

                String jsonString;
                ObjectMapper objectMapper = new ObjectMapper();
                jsonString = JsonFormat.printer().print(request);
                // log.info("FeatureImportanceRequest: \n" + jsonString);

                ManagedChannel channel = ManagedChannelBuilder.forAddress(grpcHostName,
                                Integer.parseInt(grpcHostPort))
                                .usePlaintext()
                                .build();

                ExplanationsBlockingStub stub = ExplanationsGrpc.newBlockingStub(channel);
                FeatureImportanceResponse response = stub.getFeatureImportance(request);
                jsonString = JsonFormat.printer().print(response);

                channel.shutdown();

                return objectMapper.readTree(jsonString);
        }

}
