package gr.imsi.athenarc.xtremexpvisapi.service.explainability;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import gr.imsi.athenarc.xtremexpvisapi.service.DataService;
import gr.imsi.athenarc.xtremexpvisapi.service.FileService;
import gr.imsi.athenarc.xtremexpvisapi.service.mlevaluation.ModelEvaluationService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.java.Log;

@Service
@Log
public class ExplainabilityService extends ExplanationsImplBase {

    @Value("${app.grpc.host.name}")
    String grpcHostName;

    @Value("${app.grpc.host.port}")
    String grpcHostPort;

    @Value("${app.file.cache.directory}")
    private String workingDirectory;

    @Value("${app.mock.ml-evaluation.path:}")
    private String mockEvaluationPath;

    DataService dataService;
    FileService fileService;
    ModelEvaluationService modelEvaluationService;
    private static final Logger LOG = LoggerFactory.getLogger(ExplainabilityService.class);

    public ExplainabilityService(DataService dataService, FileService fileService,
            ModelEvaluationService modelEvaluationService) {
        this.dataService = dataService;
        this.fileService = fileService;
        this.modelEvaluationService = modelEvaluationService;
    }

    public JsonNode GetExplains(String explainabilityRequest,
            String experimentId, String runId)
            throws InvalidProtocolBufferException, JsonProcessingException {

        ExplanationsRequest.Builder requestBuilder = ExplanationsRequest.newBuilder();
        JsonFormat.parser().merge(explainabilityRequest, requestBuilder);
        Optional<Map<String, Path>> loadedPaths = modelEvaluationService.loadExplainabilityDataPaths(experimentId, runId);

        if (loadedPaths.isEmpty()) {
            throw new RuntimeException(
                    "Failed to load explainability data for experiment: " + experimentId + ", run: " + runId);
        }

        Map<String, String> loadedData = loadedPaths.get().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().toString()));

        requestBuilder.putAllData(loadedData);

        ExplanationsRequest request = requestBuilder.build();
        System.out.println("Request: \n" + request);

        ManagedChannel channel = ManagedChannelBuilder.forAddress(grpcHostName,
                Integer.parseInt(grpcHostPort))
                .usePlaintext()
                .build();

        ExplanationsBlockingStub stub = ExplanationsGrpc.newBlockingStub(channel);

        ExplanationsResponse response = stub.getExplanation(request);
        System.out.println("Response: \n" + response);

        channel.shutdown();

        String jsonString = JsonFormat.printer().print(response);

        ObjectMapper objectMapper = new ObjectMapper();
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
        System.out.println("Response " + response);

        // Shutdown the channel
        channel.shutdown();

        String jsonString = JsonFormat.printer().print(response);

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(jsonString);
    } 

}
