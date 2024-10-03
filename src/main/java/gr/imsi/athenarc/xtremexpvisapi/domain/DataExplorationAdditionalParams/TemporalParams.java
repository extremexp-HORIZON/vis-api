package gr.imsi.athenarc.xtremexpvisapi.domain.DataExplorationAdditionalParams;

import java.time.temporal.ChronoUnit;
 

public class TemporalParams {
    private String groupColumn;
    private ChronoUnit granularity;
    public String getGroupColumn() {
        return groupColumn;
    }
    public void setGroupColumn(String groupColumn) {
        this.groupColumn = groupColumn;
    }
    public ChronoUnit getGranularity() {
        return granularity;
    }
    public void setGranularity(ChronoUnit granularity) {
        this.granularity = granularity;
    }
    
   
}

