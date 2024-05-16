package gr.imsi.athenarc.xtremexpvisapi.domain;

public class VisualExplainabilityResults {
    private String pdpvalues;
    private String pdphpval;
    private String pdp2dxi;
    private String pdp2dyi;
    private String pdp2dzi;
    private String aledata;
    private String modelpdpvalues;
    private String modelpdpeff; 
    private String cfss;
    private String positive;
    private String negative;

    private String message;
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public String getPdpvalues() {
        return pdpvalues;
    }
    public void setPdpvalues(String pdpvalues) {
        this.pdpvalues = pdpvalues;
    }
    public String getPdphpval() {
        return pdphpval;
    }
    public void setPdphpval(String pdphpval) {
        this.pdphpval = pdphpval;
    }
    public String getPdp2dxi() {
        return pdp2dxi;
    }
    public void setPdp2dxi(String pdp2dxi) {
        this.pdp2dxi = pdp2dxi;
    }
    public String getPdp2dyi() {
        return pdp2dyi;
    }
    public void setPdp2dyi(String pdp2dyi) {
        this.pdp2dyi = pdp2dyi;
    }
    public String getPdp2dzi() {
        return pdp2dzi;
    }
    public void setPdp2dzi(String pdp2dzi) {
        this.pdp2dzi = pdp2dzi;
    }
    public String getAledata() {
        return aledata;
    }
    public void setAledata(String aledata) {
        this.aledata = aledata;
    }
    public String getModelpdpvalues() {
        return modelpdpvalues;
    }
    public void setModelpdpvalues(String modelpdpvalues) {
        this.modelpdpvalues = modelpdpvalues;
    }
    public String getModelpdpeff() {
        return modelpdpeff;
    }
    public void setModelpdpeff(String modelpdpeff) {
        this.modelpdpeff = modelpdpeff;
    }
    public String getCfss() {
        return cfss;
    }
    public void setCfss(String cfss) {
        this.cfss = cfss;
    }
    public String getPositive() {
        return positive;
    }
    public void setPositive(String positive) {
        this.positive = positive;
    }
    public String getNegative() {
        return negative;
    }
    public void setNegative(String negative) {
        this.negative = negative;
    }

   
}