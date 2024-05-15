package gr.imsi.athenarc.xtremexpvisapi.domain.VisualExplainability.Pipeline;

public class PipelineCounterfactualParameters  {

    private String feature;

    
    public PipelineCounterfactualParameters(String feature) {
        this.feature = feature;
    }
    public PipelineCounterfactualParameters() {
    }
    public String getFeature() {
        return feature;
    }
    public void setFeature(String feature) {
        this.feature = feature;
    }
 
    @Override
    public String toString() {
        return "PipelineCounterfactualParameters [feature=" + feature +  "]";
    }
    
}
