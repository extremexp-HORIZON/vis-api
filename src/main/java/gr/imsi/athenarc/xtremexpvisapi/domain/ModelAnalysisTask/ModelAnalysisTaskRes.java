package gr.imsi.athenarc.xtremexpvisapi.domain.ModelAnalysisTask;

import gr.imsi.athenarc.xtremexpvisapi.domain.InitializeProcedure.FeatureExplanation;
import lombok.Data;

@Data
public class ModelAnalysisTaskRes {
    FeatureExplanation featureExplanation;

    public ModelAnalysisTaskRes() {
    }
}
