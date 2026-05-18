package gr.imsi.athenarc.xtremexpvisapi.domain.observability;

import java.util.List;

public class TracesResponse {
    private List<Trace> data;
    private Meta meta;

    // Getters and Setters
    public List<Trace> getData() {
        return data;
    }

    public void setData(List<Trace> data) {
        this.data = data;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }
}
