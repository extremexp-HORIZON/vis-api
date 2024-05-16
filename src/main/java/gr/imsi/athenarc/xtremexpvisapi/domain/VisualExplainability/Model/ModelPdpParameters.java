package gr.imsi.athenarc.xtremexpvisapi.domain.VisualExplainability.Model;

public class ModelPdpParameters  {

    private String feature;

    
    public ModelPdpParameters(String feature) {
        this.feature = feature;
    }
    public ModelPdpParameters() {
    }
    public String getFeature() {
        return feature;
    }
    public void setFeature(String feature) {
        this.feature = feature;
    }
 
    @Override
    public String toString() {
        return "ModelPdpParameters [feature=" + feature +  "]";
    }
    
}
