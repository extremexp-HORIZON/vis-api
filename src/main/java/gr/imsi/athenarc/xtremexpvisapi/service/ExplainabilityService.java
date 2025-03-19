package gr.imsi.athenarc.xtremexpvisapi.service;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

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
import explainabilityService.hyperparameters;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

@Service
public class ExplainabilityService extends ExplanationsImplBase {

    @Value("${app.grpc.host.name}")
    String grpcHostName;

    @Value("${app.grpc.host.port}")
    String grpcHostPort;

    @Value("${app.file.cache.directory}")
    private String workingDirectory;

    DataService dataService;
    FileService fileService;

    public ExplainabilityService(DataService dataService, FileService fileService) {
        this.dataService = dataService;
        this.fileService = fileService;
    }

    public JsonNode GetExplains(String jsonRequest) throws InvalidProtocolBufferException, JsonProcessingException {

        ExplanationsRequest.Builder requestBuilder = ExplanationsRequest.newBuilder();
        JsonFormat.parser().merge(jsonRequest, requestBuilder);

        // If there are hyperconfigs, download the files from Zenoh
        if (requestBuilder.getHyperConfigsCount() != 0) {
            Map<String, hyperparameters> updatedHyperConfigs = new HashMap<>();
            requestBuilder.getHyperConfigsMap().forEach((k, v) -> {
                try {
                    fileService.downloadFileFromZenoh(k);
                    // Replace the existing key of the map to the new path
                    String newPath = workingDirectory + k;
                    updatedHyperConfigs.put(newPath, v);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            updatedHyperConfigs.forEach((newKey, value) -> {
                requestBuilder.removeHyperConfigs(newKey.substring(workingDirectory.length()));
                requestBuilder.putHyperConfigs(newKey, value);
            });
        }

        // If data is not null, download the file from Zenoh
        if (!requestBuilder.getData().isEmpty()) {
            try {
                String dataPath = requestBuilder.getData();
                fileService.downloadFileFromZenoh(requestBuilder.getData());
                String newPath = workingDirectory + dataPath;
                requestBuilder.setData(newPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // If model is not null, download the files from Zenoh
        if (requestBuilder.getModelCount() != 0) {
            IntStream.range(0, requestBuilder.getModelCount()).forEach(i -> {
                try {
                    String model = requestBuilder.getModel(i);
                    fileService.downloadFileFromZenoh(model);
                    String newPath = workingDirectory + model;
                    requestBuilder.setModel(i, newPath);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        ExplanationsRequest request = requestBuilder.build();
        System.out.println("Request: \n" + request);

        ManagedChannel channel = ManagedChannelBuilder.forAddress(grpcHostName, Integer.parseInt(grpcHostPort))
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
