package gr.imsi.athenarc.xtremexpvisapi.domain;

public class VisualizationResults {

    private String message;
    private String data;

    public VisualizationResults() {
    }


    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
