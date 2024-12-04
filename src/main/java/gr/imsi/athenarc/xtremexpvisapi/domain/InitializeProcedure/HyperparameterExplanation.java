package gr.imsi.athenarc.xtremexpvisapi.domain.InitializeProcedure;

import java.util.List;
import java.util.Map;

import gr.imsi.athenarc.xtremexpvisapi.domain.ExplabilityProcedure.ExplanationsRes;
import lombok.Data;;

@Data
public class HyperparameterExplanation {

    private List<String> hyperparameterNames;
    private Map<String, ExplanationsRes> plots;
    private Map<String, ExplanationsRes> tables;
    private String pipelineMetrics;    
}
