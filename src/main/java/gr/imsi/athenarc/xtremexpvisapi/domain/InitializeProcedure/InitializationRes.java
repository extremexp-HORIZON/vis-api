package gr.imsi.athenarc.xtremexpvisapi.domain.InitializeProcedure;

public class InitializationRes {
    private FeatureExplanation featureExplanation;
    private HyperparameterExplanation hyperparameterExplanation;

    public FeatureExplanation getFeatureExplanation() {
        return featureExplanation;
    }

    public void setFeatureExplanation(FeatureExplanation featureExplanation) {
        this.featureExplanation = featureExplanation;
    }

    public HyperparameterExplanation getHyperparameterExplanation() {
        return hyperparameterExplanation;
    }

    public void setHyperparameterExplanation(HyperparameterExplanation hyperparameterExplanation) {
        this.hyperparameterExplanation = hyperparameterExplanation;
    }

    @Override
    public String toString() {
        return "Initialization{" +
         "featureExplanation=" + featureExplanation +
                ", hyperparameterExplanation=" + hyperparameterExplanation +
                
                
                '}';
    }


    
}
