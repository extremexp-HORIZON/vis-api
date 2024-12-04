package gr.imsi.athenarc.xtremexpvisapi.domain.InitializeProcedure;

import java.util.Map;

import gr.imsi.athenarc.xtremexpvisapi.domain.ExplabilityProcedure.ExplanationsRes;
import lombok.Data;

@Data
public class FeatureExplanation {

    private String[] featureNames;
    private Map<String, ExplanationsRes> plots;
    private Map<String, ExplanationsRes> tables;
    private String modelInstances;
    private String modelConfusionMatrix;       
}
