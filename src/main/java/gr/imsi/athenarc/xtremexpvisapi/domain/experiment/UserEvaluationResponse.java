package gr.imsi.athenarc.xtremexpvisapi.domain.experiment;

public class UserEvaluationResponse {
    private String status;
    private String message;

    public UserEvaluationResponse() {
    }
    
    public UserEvaluationResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    @Override
    public String toString() {
        return "UserEvaluationResponse{" +
                "status='" + status + '\'' +
                ", message='" + message + '\'' +
                '}';
    }

}

