package gr.imsi.athenarc.xtremexpvisapi.domain.Explainability;

import lombok.Data;

@Data
public class ExplanationsReq {
    private String explanationType;
    private String explanationMethod;
    private Integer modelId;
    private String model;
    private String feature1;
    private String feature2;
    private String query;
    private String target;
    private Integer gcfSize;
    private String cfGenerator;
    private String clusterActionChoiceAlgo;
}
