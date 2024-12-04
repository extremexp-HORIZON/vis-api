package gr.imsi.athenarc.xtremexpvisapi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;

import explainabilityService.ExplanationsGrpc;
import explainabilityService.ExplanationsGrpc.ExplanationsBlockingStub;
import explainabilityService.ExplanationsGrpc.ExplanationsImplBase;
import explainabilityService.ExplanationsRequest;
import explainabilityService.ExplanationsResponse;
import explainabilityService.InitializationRequest;
import explainabilityService.InitializationResponse;
import explainabilityService.ModelAnalysisTaskRequest;
import explainabilityService.ModelAnalysisTaskResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.ExplabilityProcedure.ExplanationsReq;
import gr.imsi.athenarc.xtremexpvisapi.domain.ExplabilityProcedure.ExplanationsRes;
import gr.imsi.athenarc.xtremexpvisapi.domain.InitializeProcedure.FeatureExplanation;
import gr.imsi.athenarc.xtremexpvisapi.domain.InitializeProcedure.InitializationReq;
import gr.imsi.athenarc.xtremexpvisapi.domain.InitializeProcedure.InitializationRes;
import gr.imsi.athenarc.xtremexpvisapi.domain.ModelAnalysisTask.ModelAnalysisTaskReq;
import gr.imsi.athenarc.xtremexpvisapi.domain.ModelAnalysisTask.ModelAnalysisTaskRes;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

@Service
public class ExplainabilityService extends ExplanationsImplBase{

    @Value("${app.grpc.host.name}")
    String grpcHostName = "";

    @Value("${app.grpc.host.port}")
    String grpcHostPort = "";

    DataService dataService;

    public ExplainabilityService(DataService dataService) {
        this.dataService = dataService;
    }
    
    public ExplanationsRes GetExplains (ExplanationsReq req) throws InvalidProtocolBufferException, JsonProcessingException {
        ExplanationsRequest request = ExplanationsRequest.newBuilder()
        .setExplanationType(req.getExplanationType())
        .setExplanationMethod(req.getExplanationMethod())
        .setModel(req.getModel())
        .setFeature1(req.getFeature1())
        .setFeature2(req.getFeature2())
        .setModelId(req.getModelId())
        // .setNumInfluential(req.getNumInfluential())
        // .setProxyDataset(req.getProxyDataset())
        // .setQuery(req.getQuery())
        // .setTarget(req.getTarget())
        // .setFeatures(req.getFeatures())
        .build();
        // ManagedChannel channel = ManagedChannelBuilder.forAddress("leviathan.imsi.athenarc.gr", 50051)
        ManagedChannel channel = ManagedChannelBuilder.forAddress(grpcHostName, Integer.parseInt(grpcHostPort))

        .usePlaintext()
        .build();

        ExplanationsBlockingStub stub = ExplanationsGrpc.newBlockingStub(channel);

        // Invoke the remote method on the target server
        
        ExplanationsResponse response = stub.getExplanation(request);
        System.out.println("Response " + response);
        // Convert the response to JSON string
        String json = JsonFormat.printer().print(response);
        System.out.println("Raw JSON response: " + json);


        // Create an ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();

        // Deserialize the JSON string into a Response object
        ExplanationsRes responseObject = objectMapper.readValue(json, ExplanationsRes.class);

        // Shutdown the channel
        channel.shutdown();

        return responseObject;

        
    }
    
    public FeatureExplanation GetModelAnalysisTask (ModelAnalysisTaskReq req) throws InvalidProtocolBufferException, JsonProcessingException {
        ModelAnalysisTaskRequest request = ModelAnalysisTaskRequest.newBuilder()
        .setModelName(req.getModelName())
        .setModelId(req.getModelId())
        .build();
        // ManagedChannel channel = ManagedChannelBuilder.forAddress("leviathan.imsi.athenarc.gr", 50051)
        ManagedChannel channel = ManagedChannelBuilder.forAddress(grpcHostName, Integer.parseInt(grpcHostPort))

        .usePlaintext()
        .build();

        ExplanationsBlockingStub stub = ExplanationsGrpc.newBlockingStub(channel);

        // Invoke the remote method on the target server
        
        ModelAnalysisTaskResponse response = stub.modelAnalysisTask(request);
        // Convert the response to JSON string
        String json = JsonFormat.printer().print(response);


        // Create an ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println("Raw object response: " + objectMapper);


        // Deserialize the JSON string into a Response object
        ModelAnalysisTaskRes responseObject = objectMapper.readValue(json, ModelAnalysisTaskRes.class);

        // Shutdown the channel
        channel.shutdown();

        return responseObject.getFeatureExplanation();

        
    }




}
