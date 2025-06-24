package gr.imsi.athenarc.xtremexpvisapi.domain.queryv2.params.aggregation;

public enum AggregationFunction {
    // Basic aggregations
    COUNT,
    COUNT_ALL,
    SUM,
    AVG,
    MIN,
    MAX,
    
    // Statistical functions
    STDDEV,
    VARIANCE,
    MEDIAN,
    
    // Percentiles
    PERCENTILE,
    
    // String aggregations
    STRING_AGG,
    ARRAY_AGG,
    
    // Advanced
    FIRST,
    LAST,
    MODE
}