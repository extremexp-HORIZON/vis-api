package gr.imsi.athenarc.xtremexpvisapi.service.explainability;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;

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

        @Autowired
        public ExplainabilityService(@Value("${app.grpc.host.name}") String grpcHostName,
                        @Value("${app.grpc.host.port}") String grpcHostPort,
                        ExplainabilityRunHelper explainabilityRunHelper) {
                this.grpcHostName = grpcHostName;
                this.grpcHostPort = grpcHostPort;
                this.explainabilityRunHelper = explainabilityRunHelper;
        }

        public JsonNode GetExplains(String explainabilityRequest,
                        String experimentId, String runId, String authorization)
                        throws InvalidProtocolBufferException, JsonProcessingException {

                ExplanationsRequest request = explainabilityRunHelper.requestBuilder(explainabilityRequest,
                                experimentId, runId, authorization);
                String jsonString;
                ObjectMapper objectMapper = new ObjectMapper();
                // print the request as JSON
                jsonString = JsonFormat.printer().print(request);
                log.info("Request: \n" + jsonString);

                ManagedChannel channel = ManagedChannelBuilder.forAddress(grpcHostName,
                                Integer.parseInt(grpcHostPort))
                                .usePlaintext()
                                .build();

                ExplanationsBlockingStub stub = ExplanationsGrpc.newBlockingStub(channel);

                ExplanationsResponse response = stub.getExplanation(request);
                jsonString = JsonFormat.printer().print(response);
                log.info("Response: \n" + jsonString);

                channel.shutdown();

                return objectMapper.readTree(jsonString);
        }

        public JsonNode ApplyAffectedActions()
                        throws InvalidProtocolBufferException, JsonProcessingException {
                ApplyAffectedActionsRequest request = ApplyAffectedActionsRequest.newBuilder()
                                .build();

                ManagedChannel channel = ManagedChannelBuilder.forAddress(grpcHostName, Integer.parseInt(grpcHostPort))
                                .usePlaintext()
                                .build();

                ExplanationsBlockingStub stub = ExplanationsGrpc.newBlockingStub(channel);

                ApplyAffectedActionsResponse response = stub.applyAffectedActions(request);

                // Shutdown the channel
                channel.shutdown();

                String jsonString = JsonFormat.printer().print(response);

                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readTree(jsonString);
        }

        public JsonNode getFeatureImportance(String featureImportanceRequest)
                        throws InvalidProtocolBufferException, JsonProcessingException {

                // 1. Build the Protobuf Request from the incoming JSON string
                FeatureImportanceRequest.Builder requestBuilder = FeatureImportanceRequest.newBuilder();
                JsonFormat.parser().ignoringUnknownFields().merge(featureImportanceRequest, requestBuilder);
                FeatureImportanceRequest request = requestBuilder.build();

                log.info("Built Feature Importance Request: \n" + request.toString());

                // 2. Create the gRPC channel and stub
                ManagedChannel channel = ManagedChannelBuilder.forAddress(grpcHostName, Integer.parseInt(grpcHostPort))
                                .usePlaintext()
                                .build();
                ExplanationsBlockingStub stub = ExplanationsGrpc.newBlockingStub(channel);

                // 3. Call the gRPC method
                FeatureImportanceResponse response = stub.getFeatureImportance(request);

                // 4. Shutdown the channel
                channel.shutdown();

                // 5. Convert the Protobuf Response to a JSON string and return
                String jsonString = JsonFormat.printer().print(response);
                log.info("Received Feature Importance Response: \n" + jsonString);

                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readTree(jsonString);
        }

}
