package gr.imsi.athenarc.xtremexpvisapi.domain.VisualExplainability.Pipeline;

public class PipelineInfluenceParameters  {

    private String feature;

    
    public PipelineInfluenceParameters(String feature) {
        this.feature = feature;
    }
    public PipelineInfluenceParameters() {
    }
    public String getFeature() {
        return feature;
    }
    public void setFeature(String feature) {
        this.feature = feature;
    }
 
    @Override
    public String toString() {
        return "PipelineInfluenceParameters [feature=" + feature +  "]";
    }
    
}
