package gr.imsi.athenarc.xtremexpvisapi.service.explainability;

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

        DataService dataService;
        FileService fileService;
        ExplainabilityRunHelper explainabilityRunHelper;

        public ExplainabilityService(DataService dataService, FileService fileService,
                        ExplainabilityRunHelper explainabilityRunHelper) {
                this.dataService = dataService;
                this.fileService = fileService;
                this.explainabilityRunHelper = explainabilityRunHelper;
        }

        public JsonNode GetExplains(String explainabilityRequest,
                        String experimentId, String runId)
                        throws InvalidProtocolBufferException, JsonProcessingException {

                ExplanationsRequest request = explainabilityRunHelper.requestBuilder(explainabilityRequest,
                                experimentId, runId);
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
                System.out.println("Response " + response);

                // Shutdown the channel
                channel.shutdown();

                String jsonString = JsonFormat.printer().print(response);

                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readTree(jsonString);
        }

}
