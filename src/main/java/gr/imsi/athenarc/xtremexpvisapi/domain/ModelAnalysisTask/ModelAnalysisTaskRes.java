package gr.imsi.athenarc.xtremexpvisapi.domain.ModelAnalysisTask;

import gr.imsi.athenarc.xtremexpvisapi.domain.InitializeProcedure.FeatureExplanation;

public class ModelAnalysisTaskRes {
    FeatureExplanation featureExplanation;

    public ModelAnalysisTaskRes() {
    }

    public FeatureExplanation getFeatureExplanation() {
        return featureExplanation;
    }

    public void setFeatureExplanation(FeatureExplanation featureExplanation) {
        this.featureExplanation = featureExplanation;
    }
}
