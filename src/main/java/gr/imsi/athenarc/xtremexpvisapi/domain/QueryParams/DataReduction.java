package gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams;

import java.util.Map;

import lombok.Data;


@Data
public class DataReduction {
    private String type; // raw, aggregation, visualization-aware
    private String aggInterval; 
    private Map<String, String> aggFunctions; 
    private ViewPort viewPort; 
    private double errorBound; 
}
