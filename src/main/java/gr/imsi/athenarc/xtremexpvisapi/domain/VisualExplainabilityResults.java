package gr.imsi.athenarc.xtremexpvisapi.domain;

public class VisualExplainabilityResults {

    private String message;
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
   
    ////MESSAGE TEXT

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    ////PIPELINE PDP

    public String getVals() {
        return pdpvalues;
    }
    public void setVals(String pdpvalues) {
        this.pdpvalues = pdpvalues;
    }

    public String getHp() {
        return pdphpval;
    }
    public void setHp(String pdphpval) {
        this.pdphpval = pdphpval;
    }

    ////PIPELINE 2D PDP
    public String getPDP2dXI() {
        return pdp2dxi;
    }
    public void setPDP2dXI(String pdp2dxi) {
        this.pdp2dxi = pdp2dxi;
    }

    public String getPDP2dYI() {
        return pdp2dyi;
    }
    public void setPDP2dYI(String pdp2dyi) {
        this.pdp2dyi = pdp2dyi;
    }

    public String getPDP2dZI() {
        return pdp2dzi;
    }
    public void setPDP2dZI(String pdp2dzi) {
        this.pdp2dzi = pdp2dzi;
    }
    ////ALE
    public String getAle() {
        return aledata;
    }
    public void setAle(String aledata) {
        this.aledata = aledata;
    }


    ////PDP MODEL 
    public String getModelVal() {
        return modelpdpvalues;
    }
    public void setModelVal(String modelpdpvalues) {
        this.modelpdpvalues = modelpdpvalues;
    }

    public String getEffect() {
        return modelpdpeff;
    }
    public void setEffect(String modelpdpeff) {
        this.modelpdpeff = modelpdpeff;
    }
    ////CFS
    public String getCfs() {
        return cfss;
    }
    public void setCfs(String cfss) {
        this.cfss = cfss;
    }
////influence
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






// public List<Double> getData() {
    //     return data;
    // }
    // public void setData(List<Double> list) {
    //     this.data = list;
    // }
    //// kane mia abstact classh kai oi alloes gia ta alla repsosnes na thn kanoun extend 
    // public String getData() {
    //     return data;
    // }
    // public void setData(String data) {
    //     this.data = data;
    // }

    ////