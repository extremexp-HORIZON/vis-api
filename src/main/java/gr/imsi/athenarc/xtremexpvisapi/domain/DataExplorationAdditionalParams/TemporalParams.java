package gr.imsi.athenarc.xtremexpvisapi.domain.DataExplorationAdditionalParams;

import java.time.temporal.ChronoUnit;

import lombok.Data;
 
@Data
public class TemporalParams {
    private String groupColumn;
    private ChronoUnit granularity;    
   
}

