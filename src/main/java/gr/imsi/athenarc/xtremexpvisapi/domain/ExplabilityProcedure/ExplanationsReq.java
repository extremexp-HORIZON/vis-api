package gr.imsi.athenarc.xtremexpvisapi.domain.ExplabilityProcedure;

public class ExplanationsReq {
    private String explanationType ;
    private String explanationMethod ;
    private String model ;
    private String feature1 ;
    private String feature2 ;
    private int numInfluential;
    private byte[] proxyDataset;
    private byte[] query;
    private String features ;
    private String target ;


    
    public int getNumInfluential() {
        return numInfluential;
    }

    public void setNumInfluential(int numInfluential) {
        this.numInfluential = numInfluential;
    }

    public byte[] getProxyDataset() {
        return proxyDataset;
    }

    public void setProxyDataset(byte[] proxyDataset) {
        this.proxyDataset = proxyDataset;
    }

    public byte[] getQuery() {
        return query;
    }

    public void setQuery(byte[] query) {
        this.query = query;
    }



    // Getters and Setters
    public String getExplanationType() {
        return explanationType;
    }

    public void setExplanationType(String explanationType) {
        this.explanationType = explanationType;
    }

    public String getExplanationMethod() {
        return explanationMethod;
    }

    public void setExplanationMethod(String explanationMethod) {
        this.explanationMethod = explanationMethod;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
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

        public String getFeatures() {
        return features;
    }

    public void setFeatures(String features) {
        this.features = features;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    @Override
    public String toString() {
        return "ExplanationsRequest{" +
               "explanationType='" + explanationType + '\'' +
               ", explanationMethod='" + explanationMethod + '\'' +
               ", model='" + model + '\'' +
               ", feature1='" + feature1 + '\'' +
               ", feature2='" + feature2 + '\'' +
               ", numInfluential=" + numInfluential +
               ", proxyDataset='" + proxyDataset + '\'' +
               ", query='" + query + '\'' +
               ", features='" + features + '\'' +
               ", target='" + target + '\'' +
               '}';
    }
}
