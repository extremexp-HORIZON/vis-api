package gr.imsi.athenarc.xtremexpvisapi.domain.VisualExplainability.Model;

public class ModelAleParameters  {

    private String feature;

    
    public ModelAleParameters(String feature) {
        this.feature = feature;
    }
    public ModelAleParameters() {
    }
    public String getFeature() {
        return feature;
    }
    public void setFeature(String feature) {
        this.feature = feature;
    }
 
    @Override
    public String toString() {
        return "ModelAleParameters [feature=" + feature +  "]";
    }
    
}
