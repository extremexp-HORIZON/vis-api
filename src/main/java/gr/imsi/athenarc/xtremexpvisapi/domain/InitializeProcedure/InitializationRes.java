package gr.imsi.athenarc.xtremexpvisapi.domain.InitializeProcedure;

import lombok.Data;

@Data
public class InitializationRes {
    private FeatureExplanation featureExplanation;
    private HyperparameterExplanation hyperparameterExplanation;

}
