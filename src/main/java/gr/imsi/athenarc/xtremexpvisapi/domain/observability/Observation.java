package gr.imsi.athenarc.xtremexpvisapi.domain.observability;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class Observation {
    private String id;
    private String traceId;
    private String type;
    private String name;
    private String startTime;
    private String endTime;
    private String completionStartTime;
    private String model;
    private Map<String, Object> modelParameters;
    private Object input;
    private String version;
    private Map<String, Object> metadata;
    private Object output;
    private Usage usage;
    private String level;
    private String statusMessage;
    private String parentObservationId;
    private String promptId;
    private Map<String, Integer> usageDetails;
    private Map<String, Integer> costDetails;
    private String environment;

    // Getters and Setters

    public static class Usage {
        private int input;
        private int output;
        private int total;
        private String unit;
        private Double inputCost;
        private Double outputCost;
        private Double totalCost;

        // Getters and Setters
    }
}
