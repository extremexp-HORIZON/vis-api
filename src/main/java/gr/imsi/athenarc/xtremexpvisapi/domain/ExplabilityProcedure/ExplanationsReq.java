package gr.imsi.athenarc.xtremexpvisapi.domain.ExplabilityProcedure;

import lombok.Data;

@Data
public class ExplanationsReq {
    private String explanationType ;
    private String explanationMethod ;
    private String model ;
    private String feature1 ;
    private String feature2 ;
    private Integer modelId ;
    private int numInfluential;
    private byte[] proxyDataset;
    private byte[] query;
    private String features ;
    private String target ;
}
