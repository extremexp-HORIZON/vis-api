package gr.imsi.athenarc.xtremexpvisapi.domain;

import java.util.Map;

import lombok.Data;


@Data
public class DataReduction {
    private String type; // raw, aggregation, visualization-aware
    private String aggInterval; 
    private Map<String, String> aggFunctions; 
    private ViewPort viewport; 
    private double errorBound; 
}
