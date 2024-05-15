package gr.imsi.athenarc.xtremexpvisapi.domain.VisualExplainability.Pipeline;

public class PipelineAleParameters  {

    private String feature;

    
    public PipelineAleParameters(String feature) {
        this.feature = feature;
    }
    public PipelineAleParameters() {
    }
    public String getFeature() {
        return feature;
    }
    public void setFeature(String feature) {
        this.feature = feature;
    }
 
    @Override
    public String toString() {
        return "PipelineAleParameters [feature=" + feature +  "]";
    }
    
}
