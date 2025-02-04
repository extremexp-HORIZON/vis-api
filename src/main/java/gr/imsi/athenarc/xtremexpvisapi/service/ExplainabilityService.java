package gr.imsi.athenarc.xtremexpvisapi.service;

import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;

import explainabilityService.ApplyAffectedActionsRequest;
import explainabilityService.ApplyAffectedActionsResponse;
import explainabilityService.ExplanationsGrpc;
import explainabilityService.ExplanationsGrpc.ExplanationsBlockingStub;
import explainabilityService.ExplanationsGrpc.ExplanationsImplBase;
import explainabilityService.ExplanationsRequest;
import explainabilityService.ExplanationsResponse;
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

    public ExplanationsResponse GetExplains(String jsonRequest) throws InvalidProtocolBufferException, JsonProcessingException {

        ExplanationsRequest.Builder requestBuilder = ExplanationsRequest.newBuilder();
        JsonFormat.parser().merge(jsonRequest, requestBuilder);
        ExplanationsRequest request = requestBuilder.build();

        // If there are hyperconfigs, download the files from Zenoh
        if(request.getHyperConfigsCount() != 0) {
        request.getHyperConfigsMap().forEach((k, v) -> {
            try {
                fileService.downloadFileFromZenoh(k);
                // Replace the existing key of the map to the new path
                String newPath =  workingDirectory + k;
                requestBuilder.removeHyperConfigs(k);
                requestBuilder.putHyperConfigs(newPath, v);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        }

        // If data is not null, download the file from Zenoh
        if(request.getData() != null) {
            try {
                String dataPath = request.getData();
                fileService.downloadFileFromZenoh(request.getData());
                String newPath =  workingDirectory + dataPath;
                requestBuilder.setData(newPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // If model is not null, download the files from Zenoh
        if(request.getModelCount() != 0) {
            IntStream.range(0, request.getModelCount()).forEach(i -> {
                try {
                    String model = request.getModel(i);
                    fileService.downloadFileFromZenoh(model);
                    String newPath =  workingDirectory + model;
                    requestBuilder.setModel(i, newPath);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        ManagedChannel channel = ManagedChannelBuilder.forAddress(grpcHostName, Integer.parseInt(grpcHostPort))
                .usePlaintext()
                .build();

        ExplanationsBlockingStub stub = ExplanationsGrpc.newBlockingStub(channel);

        ExplanationsResponse response = stub.getExplanation(request);
        System.out.println("Response: \n" + response);

        channel.shutdown();

        return response;
    }

    public ApplyAffectedActionsResponse ApplyAffectedActions() throws InvalidProtocolBufferException, JsonProcessingException {
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

        return response;

    }

}
