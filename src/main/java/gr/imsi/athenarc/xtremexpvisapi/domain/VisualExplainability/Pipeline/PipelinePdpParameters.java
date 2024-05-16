package gr.imsi.athenarc.xtremexpvisapi.domain.VisualExplainability.Pipeline;

public class PipelinePdpParameters  {

    private String feature;

    
    public PipelinePdpParameters(String feature) {
        this.feature = feature;
    }
    public PipelinePdpParameters() {
    }
    public String getFeature() {
        return feature;
    }
    public void setFeature(String feature) {
        this.feature = feature;
    }
 
    @Override
    public String toString() {
        return "PipelinePdpParameters [feature=" + feature +  "]";
    }
    
}
