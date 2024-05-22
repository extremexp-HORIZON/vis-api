package gr.imsi.athenarc.xtremexpvisapi.domain.InitializeProcedure;

public class InitializationRes {
    private Feature_Explanation featureExplanation;
    private Hyperparameter_Explanation hyperparameterExplanation;

    public Feature_Explanation getFeatureExplanation() {
        return featureExplanation;
    }

    public void setFeatureExplanation(Feature_Explanation featureExplanation) {
        this.featureExplanation = featureExplanation;
    }

    public Hyperparameter_Explanation getHyperparameterExplanation() {
        return hyperparameterExplanation;
    }

    public void setHyperparameterExplanation(Hyperparameter_Explanation hyperparameterExplanation) {
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
