package gr.imsi.athenarc.xtremexpvisapi.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ibm.icu.impl.locale.LocaleDistance.Data;

import gr.imsi.athenarc.xtremexpvisapi.controller.VisualizationController;
import gr.imsi.athenarc.xtremexpvisapi.datasource.QueryExecutor;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualColumn;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualExplainabilityResults;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualizationResults;
import gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.ExplanationsGrpc;
import gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.ExplanationsRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.ExplanationsResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualExplainabilityModel2DPdpQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualExplainabilityModelAleQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualExplainabilityModelPdpQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualExplainabilityPipeline2DPdpQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualExplainabilityPipelineAleQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualExplainabilityPipelineCounterfactualQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualExplainabilityPipelineInfluenceQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualExplainabilityPipelinePdpQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualQuery;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

@Service
public class DataService {

    private static final Logger LOG = LoggerFactory.getLogger(DataService.class);

    
    @Value("${app.schema.path}")
    String schemaPath = "";
    
/*
 * General Fucntions for Datas
 * 
 * 
 */
public static List<Double> stringToDoubleList(String valuesString) {
    // Remove enclosing square brackets and split by comma
    String[] values = valuesString.substring(2, valuesString.length() - 2).split(", ");

    List<Double> doubleList = new ArrayList<>();

    // Convert each string element to a double and add to the list
    for (String value : values) {
        doubleList.add(Double.parseDouble(value));
    }

    return doubleList;
}

    public VisualizationResults getData(VisualQuery visualQuery) {
        LOG.info("Retrieving columns for datasetId: {}", visualQuery.getDatasetId());

        VisualizationResults visualizationResults = new VisualizationResults();
        String datasetId = visualQuery.getDatasetId();

        // Print datasetId being processed
        LOG.info("Processing data for datasetId: {}", datasetId);

        if (visualQuery.getFilters().contains(null)) {
            visualizationResults.setMessage("500");
            LOG.warn("Null filter detected in visualQuery");
            return visualizationResults;
        }

        QueryExecutor queryExecutor = new QueryExecutor(datasetId, Path.of(schemaPath, datasetId + ".csv").toString());

        // Print executing query with visualQuery
        LOG.info("Executing.. query for datasetId: {}", datasetId);
        return queryExecutor.executeQuery(visualQuery);
    }

    public List<VisualColumn> getColumns(String datasetId) {
        LOG.info("Retrieving columns for datasetId: {}", datasetId);
        QueryExecutor queryExecutor = new QueryExecutor(datasetId, Path.of(schemaPath, datasetId + ".csv").toString());
        return queryExecutor.getColumns(datasetId);
    }

    public String getColumn(String datasetId, String columnName) {
        LOG.info("Retrieving column {} for datasetId: {}", columnName, datasetId);
        QueryExecutor queryExecutor = new QueryExecutor(datasetId, Path.of(schemaPath, datasetId + ".csv").toString());
        return queryExecutor.getColumn(datasetId, columnName);
    }


/*
 * Pipeline XAI Fucntions 
 */

    public VisualExplainabilityResults getPipelineExplainabilityPdpData(VisualExplainabilityPipelinePdpQuery visualExplainabilityQuery) {
        VisualExplainabilityResults visualExplainabilityResults = new VisualExplainabilityResults();
        String feature = visualExplainabilityQuery.getPipelinePdpParameters().getFeature();
        
        // Check if the feature is null
        if (feature == null) {
            visualExplainabilityResults.setMessage("400");
            LOG.warn("Warning: Feature is null in visualExplainabilityQuery");
        } else {
            visualExplainabilityResults.setMessage("200");
            LOG.info("Sending gRPC request to server...");
    
            // Establish gRPC channel
            ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext() // Use plaintext (non-TLS) communication
                .build();
           
            // Create a request message
            ExplanationsRequest request = ExplanationsRequest.newBuilder()
                    .setExplanationMethod("PDPlots")
                    .setExplanationType("Pipeline")
                    .setModel(visualExplainabilityQuery.getModelId())
                    .setFeature1(feature) // Use the extracted feature
                    .build();
           
            // Create a blocking stub for the service
            ExplanationsGrpc.ExplanationsBlockingStub stub = ExplanationsGrpc.newBlockingStub(channel);
           
            // Send the request and get the response
            ExplanationsResponse response = stub.getExplanation(request);
            // System.out.println(response.getP);
            visualExplainabilityResults.setHp(response.getPdpHpValues());
            visualExplainabilityResults.setVals(response.getPdpValues());

            // Shutdown the channel
            channel.shutdown();
        }
        
        return visualExplainabilityResults;
    }
   
    public VisualExplainabilityResults getPipelineExplainability2DPdpData(VisualExplainabilityPipeline2DPdpQuery visualExplainabilityQuery) {
        VisualExplainabilityResults visualExplainabilityResults = new VisualExplainabilityResults();
        String feature1 = visualExplainabilityQuery.getPipelinePdpParameters().getFeature1();
        String feature2 = visualExplainabilityQuery.getPipelinePdpParameters().getFeature2();
        
        // Check if either feature1 or feature2 is null
        if (feature1 == null || feature2 == null) {
            visualExplainabilityResults.setMessage("400");
            LOG.warn("Warning: One or both features are null in visualExplainabilityQuery");
        } else {
            visualExplainabilityResults.setMessage("200");
            LOG.info("Sending gRPC request to server...");
    
            // Establish gRPC channel
            ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext() // Use plaintext (non-TLS) communication
                .build();
           
            // Create a request message
            ExplanationsRequest request = ExplanationsRequest.newBuilder()
                    .setExplanationMethod("2D_PDPlots")
                    .setExplanationType("Pipeline")
                    .setModel(visualExplainabilityQuery.getModelId())
                    .setFeature1(feature1) // Use the extracted feature1
                    .setFeature2(feature2) // Use the extracted feature2
                    .build();
           
            // Create a blocking stub for the service
            ExplanationsGrpc.ExplanationsBlockingStub stub = ExplanationsGrpc.newBlockingStub(channel);

            // Send the request and get the response
            ExplanationsResponse response = stub.getExplanation(request);
            
            visualExplainabilityResults.setPdp2dxi(response.getPdp2DXi());
            visualExplainabilityResults.setPdp2dyi(response.getPdp2DYi());
            visualExplainabilityResults.setPdp2dzi(response.getPdp2DZi());

            // Shutdown the channel
            channel.shutdown();
        }
        
        return visualExplainabilityResults;
    }
    
    public VisualExplainabilityResults getPipelineExplainabilityAleData(VisualExplainabilityPipelineAleQuery visualExplainabilityQuery) {
        VisualExplainabilityResults visualExplainabilityResults = new VisualExplainabilityResults();
        String feature = visualExplainabilityQuery.getPipelineAleParameters().getFeature();
        
        // Check if the feature is null
        if (feature == null) {
            LOG.warn("Feature is null");
            visualExplainabilityResults.setMessage("400");
        } else {
            visualExplainabilityResults.setMessage("200");
            LOG.info("Sending gRPC request to server...");
    
            // Establish gRPC channel
            ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext() // Use plaintext (non-TLS) communication
                .build();
           
            // Create a request message
            ExplanationsRequest request = ExplanationsRequest.newBuilder()
                    .setExplanationMethod("ALEPlots")
                    .setExplanationType("Pipeline")
                    .setModel(visualExplainabilityQuery.getModelId())
                    .setFeature1(feature) // Use the extracted feature
                    .build();
           
            // Create a blocking stub for the service
            ExplanationsGrpc.ExplanationsBlockingStub stub = ExplanationsGrpc.newBlockingStub(channel);
           
            // Send the request and get the response
            ExplanationsResponse response = stub.getExplanation(request);
            
            visualExplainabilityResults.setAledata(response.getAleData());

            // Shutdown the channel
            channel.shutdown();
        }
        
        return visualExplainabilityResults;
    }

    public VisualExplainabilityResults getPipelineExplainabilityCounterFactualData(VisualExplainabilityPipelineCounterfactualQuery visualExplainabilityQuery) {
        VisualExplainabilityResults visualExplainabilityResults = new VisualExplainabilityResults();
        String feature = visualExplainabilityQuery.getPipelineCounterfactualParameters().getFeature();
        
        if (feature == null) {
            LOG.warn("Feature is null");
            visualExplainabilityResults.setMessage("400");
        } else {
            visualExplainabilityResults.setMessage("200");
            LOG.info("Sending gRPC request to server...");
    
            // Establish gRPC channel
            ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext() // Use plaintext (non-TLS) communication
                .build();
           
            // Create a request message
            ExplanationsRequest request = ExplanationsRequest.newBuilder()
                    .setExplanationMethod("CounterfactualExplanations")
                    .setExplanationType("Pipeline")
                    .setModel(visualExplainabilityQuery.getModelId())
                    // .setFeature1(feature) // Optionally set the feature if needed
                    .build();
            
            // Create a blocking stub for the service
            ExplanationsGrpc.ExplanationsBlockingStub stub = ExplanationsGrpc.newBlockingStub(channel);
           
            // Send the request and get the response
            ExplanationsResponse response = stub.getExplanation(request);
            
            visualExplainabilityResults.setCfss(response.getCfs());

            // Shutdown the channel
            channel.shutdown();
        }
    
        return visualExplainabilityResults;
    }
   
    public VisualExplainabilityResults getPipelineExplainabilityInfluenceData(VisualExplainabilityPipelineInfluenceQuery visualExplainabilityQuery) {
        VisualExplainabilityResults visualExplainabilityResults = new VisualExplainabilityResults();
        Integer noOfInfluential = visualExplainabilityQuery.getPipelineInfluenceParameters().getNoOfInfluential();
    
        if (noOfInfluential == null) {
            LOG.warn("noOfInfluential is null");
            visualExplainabilityResults.setMessage("400");
        } else {
            visualExplainabilityResults.setMessage("200");
            LOG.info("Sending gRPC request to server...");
    
            try {
                // Establish gRPC channel
                ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                    .usePlaintext() // Use plaintext (non-TLS) communication
                    .build();
                
                // Create a request message
                ExplanationsRequest request = ExplanationsRequest.newBuilder()
                        .setExplanationMethod("InfluenceFunctions")
                        .setExplanationType("Pipeline")
                        .setModel(visualExplainabilityQuery.getModelId())
                        .setNumInfluential(noOfInfluential) // Parse feature to int
                        .build();
                
                // Create a blocking stub for the service
                ExplanationsGrpc.ExplanationsBlockingStub stub = ExplanationsGrpc.newBlockingStub(channel);
                
                // Send the request and get the response
                ExplanationsResponse response = stub.getExplanation(request);
                visualExplainabilityResults.setPositive(response.getPositive());
                visualExplainabilityResults.setNegative(response.getNegative());
                

                channel.shutdown();
            } catch (NumberFormatException e) {
                // Handle number format exception
                System.err.println("Error parsing feature to integer: " + e.getMessage());
                visualExplainabilityResults.setMessage("400");
            } catch (Exception e) {
                // Handle other exceptions
                System.err.println("Error occurred during gRPC request: " + e.getMessage());
                visualExplainabilityResults.setMessage("500");
            }
        }
    
        return visualExplainabilityResults;
    }
       
/*
 * Model XAI Fucntions 
 */

    public VisualExplainabilityResults getModelExplainabilityPdpData(VisualExplainabilityModelPdpQuery visualExplainabilityQuery) {
        VisualExplainabilityResults visualExplainabilityResults = new VisualExplainabilityResults();
        String feature = visualExplainabilityQuery.getModelPdpParameters().getFeature();
        
        // Check if the feature is null
        if (feature == null) {
            LOG.warn("feature is null");
            visualExplainabilityResults.setMessage("400");
        } else {
            visualExplainabilityResults.setMessage("200");
            LOG.info("Sending gRPC request to server...");
    
            // Establish gRPC channel
            ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext() // Use plaintext (non-TLS) communication
                .build();
           
            // Create a request message
            ExplanationsRequest request = ExplanationsRequest.newBuilder()
                    .setExplanationMethod("PDPlots")
                    .setExplanationType("Model")
                    .setModel(visualExplainabilityQuery.getModelId())
                    .setFeature1(feature) // Use the extracted feature
                    .build();
           
            // Create a blocking stub for the service
            ExplanationsGrpc.ExplanationsBlockingStub stub = ExplanationsGrpc.newBlockingStub(channel);
           
            // Send the request and get the response
            ExplanationsResponse response = stub.getExplanation(request);
            visualExplainabilityResults.setModelpdpeff(response.getPdpEffect());
            visualExplainabilityResults.setModelpdpvalues(response.getPdpVals());
            // Shutdown the channel
            channel.shutdown();
        }
    
        return visualExplainabilityResults;
    }
    
    public VisualExplainabilityResults getModelExplainabilityAleData(VisualExplainabilityModelAleQuery visualExplainabilityQuery) {
        VisualExplainabilityResults visualExplainabilityResults = new VisualExplainabilityResults();
        String feature = visualExplainabilityQuery.getModelAleParameters().getFeature();
        
        // Check if the feature is null
        if (feature == null) {
            // Log when feature is null
            LOG.warn("Feature is null");
            visualExplainabilityResults.setMessage("400");
        } else {
            visualExplainabilityResults.setMessage("200");
            LOG.info("Sending gRPC request to server...");
    
            // Establish gRPC channel
            ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext() // Use plaintext (non-TLS) communication
                .build();
           
            // Create a request message
            ExplanationsRequest request = ExplanationsRequest.newBuilder()
                    .setExplanationMethod("ALEPlots")
                    .setExplanationType("Model")
                    .setModel(visualExplainabilityQuery.getModelId())
                    .setFeature1(feature) // Use the extracted feature
                    .build();
           
            // Create a blocking stub for the service
            ExplanationsGrpc.ExplanationsBlockingStub stub = ExplanationsGrpc.newBlockingStub(channel);
           
            // Send the request and get the response
            ExplanationsResponse response = stub.getExplanation(request);
            visualExplainabilityResults.setAledata(response.getAleData());
            // Shutdown the channel
            channel.shutdown();
        }
        
        return visualExplainabilityResults;
    }
    
    public VisualExplainabilityResults getModelExplainability2DPdpData(VisualExplainabilityModel2DPdpQuery visualExplainabilityQuery) {
        VisualExplainabilityResults visualExplainabilityResults = new VisualExplainabilityResults();
        String feature1 = visualExplainabilityQuery.getModel2DPdpParameters().getFeature1();
        String feature2 = visualExplainabilityQuery.getModel2DPdpParameters().getFeature2();
        
        // Check if either feature1 or feature2 is null
        if (feature1 == null || feature2 == null) {
            // Log when either feature1 or feature2 is null
            LOG.warn("Feature1 or Feature2 is null");
            visualExplainabilityResults.setMessage("400");
        } else {


            visualExplainabilityResults.setMessage("200");
            LOG.info("Sending gRPC request to server...");

            // Establish gRPC channel
            ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext() // Use plaintext (non-TLS) communication
                .build();
           
            // Create a request message
            ExplanationsRequest request = ExplanationsRequest.newBuilder()
                    .setExplanationMethod("2D_PDPlots")
                    .setExplanationType("Model")
                    .setModel(visualExplainabilityQuery.getModelId())
                    .setFeature1(feature1) // Use the extracted feature1
                    .setFeature2(feature2) // Use the extracted feature2
                    .build();
            // Create a blocking stub for the service
            ExplanationsGrpc.ExplanationsBlockingStub stub = ExplanationsGrpc.newBlockingStub(channel);
            // Send the request and get the response

            ExplanationsResponse response = stub.getExplanation(request);
            visualExplainabilityResults.setModelpdpeff(response.getPdpEffect());
            visualExplainabilityResults.setModelpdpvalues(response.getPdpVals() );

            // Shutdown the channel
            channel.shutdown();
        }
        return visualExplainabilityResults;
    }
    
}
