package gr.imsi.athenarc.xtremexpvisapi.domain.VisualExplainability.Model;

public class Model2DPdpParameters  {

    private String feature1;
    private String feature2;

    
    public Model2DPdpParameters(String feature1, String feature2) {
        this.feature1 = feature1;
        this.feature2 = feature2;
    }
    public Model2DPdpParameters() {
    }
    public String getFeature1() {
        return feature1;
    }
    public void setFeature1(String feature1) {
        this.feature1 = feature1;
    }
    public String getFeature2() {
        return feature2;
    }
    public void setFeature2(String feature2) {
        this.feature2 = feature2;
    }
    @Override
    public String toString() {
        return "Model2DPdpParameters [feature1=" + feature1 + ", feature2=" + feature2 + "]";
    }
    
}
