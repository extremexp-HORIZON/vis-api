package gr.imsi.athenarc.xtremexpvisapi.domain.ModelAnalysisTask;

import gr.imsi.athenarc.xtremexpvisapi.domain.InitializeProcedure.FeatureExplanation;

public class ModelAnalysisTaskRes {
    FeatureExplanation featureExplanation;

    public FeatureExplanation getFeatureExplanations() {
        return featureExplanation;
    }

    public void setFeatureExplanations(FeatureExplanation featureExplanation) {
        this.featureExplanation = featureExplanation;
    }

    public ModelAnalysisTaskRes() {
    }
}
