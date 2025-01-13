package gr.imsi.athenarc.xtremexpvisapi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;

import explainabilityService.ApplyAffectedActionsRequest;
import explainabilityService.ApplyAffectedActionsResponse;
import explainabilityService.ExplanationsGrpc;
import explainabilityService.ExplanationsGrpc.ExplanationsBlockingStub;
import explainabilityService.ExplanationsGrpc.ExplanationsImplBase;
import gr.imsi.athenarc.xtremexpvisapi.domain.Explainability.ApplyAffectedActionsRes;
import gr.imsi.athenarc.xtremexpvisapi.domain.Explainability.ExplanationsReq;
import gr.imsi.athenarc.xtremexpvisapi.domain.Explainability.ExplanationsRes;
import explainabilityService.ExplanationsRequest;
import explainabilityService.ExplanationsResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

@Service
public class ExplainabilityService extends ExplanationsImplBase{

    @Value("${app.grpc.host.name}")
    String grpcHostName;

    @Value("${app.grpc.host.port}")
    String grpcHostPort;

    DataService dataService;

    public ExplainabilityService(DataService dataService) {
        this.dataService = dataService;
    }
    
    public ExplanationsRes GetExplains (ExplanationsReq req) throws InvalidProtocolBufferException, JsonProcessingException {
        ExplanationsRequest request = ExplanationsRequest.newBuilder()
        .setExplanationType(req.getExplanationType())
        .setExplanationMethod(req.getExplanationMethod())
        .setModelId(req.getModelId())
        .setModel(req.getModel())
        .setFeature1(req.getFeature1())
        .setFeature2(req.getFeature2())
        .setQuery(req.getQuery())
        .setTarget(req.getTarget())
        .setGcfSize(req.getGcfSize())
        .setCfGenerator(req.getCfGenerator())
        .setClusterActionChoiceAlgo(req.getClusterActionChoiceAlgo())
        .build();
        
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
    
    public ApplyAffectedActionsRes ApplyAffectedActions () throws InvalidProtocolBufferException, JsonProcessingException {
        ApplyAffectedActionsRequest request = ApplyAffectedActionsRequest.newBuilder()
        .build();
        
        ManagedChannel channel = ManagedChannelBuilder.forAddress(grpcHostName, Integer.parseInt(grpcHostPort))

        .usePlaintext()
        .build();

        ExplanationsBlockingStub stub = ExplanationsGrpc.newBlockingStub(channel);

        // Invoke the remote method on the target server
        
        ApplyAffectedActionsResponse response = stub.applyAffectedActions(request);
        System.out.println("Response " + response);
        // Convert the response to JSON string
        String json = JsonFormat.printer().print(response);
        System.out.println("Raw JSON response: " + json);


        // Create an ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();

        // Deserialize the JSON string into a Response object
        ApplyAffectedActionsRes responseObject = objectMapper.readValue(json, ApplyAffectedActionsRes.class);

        // Shutdown the channel
        channel.shutdown();

        return responseObject;

    }

}
