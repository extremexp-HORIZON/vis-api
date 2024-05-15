package gr.imsi.athenarc.xtremexpvisapi.domain.VisualExplainability.Pipeline;

public class PipelineInfluenceParameters  {

    private Integer noOfInfluential;

    
    public PipelineInfluenceParameters(Integer noOfInfluential) {
        this.noOfInfluential = noOfInfluential;
    }
    public PipelineInfluenceParameters() {
    }

    public Integer getNoOfInfluential() {
        return noOfInfluential;
    }


    public void setNoOfInfluential(Integer noOfInfluential) {
        this.noOfInfluential = noOfInfluential;
    }
    @Override
    public String toString() {
        return "PipelineInfluenceParameters [noOfInfluential=" + noOfInfluential + "]";
    }
}
