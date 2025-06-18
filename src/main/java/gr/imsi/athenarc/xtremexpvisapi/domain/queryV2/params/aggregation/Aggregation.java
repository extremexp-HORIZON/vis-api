package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.aggregation;


import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Aggregation {
    private String column;
    private AggregationFunction function;
    private String alias; // Optional custom alias
    private AggregationOptions options; // For advanced options like DISTINCT, percentiles
    
    // Convenience constructor
    public Aggregation(String column, AggregationFunction function) {
        this.column = column;
        this.function = function;
        this.alias = function.name().toLowerCase() + "_" + column;
    }
    
    // Generate SQL for this aggregation
    public String toSql() {
        StringBuilder sql = new StringBuilder();
        
        switch (function) {
            case COUNT:
                if (options != null && options.isDistinct()) {
                    sql.append("COUNT(DISTINCT ").append(columnPreparation(column)).append(")");
                } else {
                    sql.append("COUNT(").append(columnPreparation(column)).append(")");
                }
                break;
                
            case COUNT_ALL:
                sql.append("COUNT(*)");
                break;
                
            case PERCENTILE:
                if (options != null && options.getPercentileValue() != null) {
                    sql.append("PERCENTILE_CONT(").append(options.getPercentileValue())
                       .append(") WITHIN GROUP (ORDER BY ").append(columnPreparation(column)).append(")");
                } else {
                    sql.append("PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY ").append(columnPreparation(column)).append(")");
                }
                break;
                
            case ARRAY_AGG:
                if (options != null && options.isDistinct()) {
                    sql.append("ARRAY_AGG(DISTINCT ").append(columnPreparation(column)).append(")");
                } else {
                    sql.append("ARRAY_AGG(").append(columnPreparation(column)).append(")");
                }
                break;
                
            default:
                // Standard aggregations: SUM, AVG, MIN, MAX, etc.
                if (options != null && options.isDistinct()) {
                    sql.append(function.name()).append("(DISTINCT ").append(columnPreparation(column)).append(")");
                } else {
                    sql.append(function.name()).append("(").append(columnPreparation(column)).append(")");
                }
                break;
        }
        
        // Add alias
        sql.append(" AS ").append(alias != null ? alias : (function.name().toLowerCase() + "_" + aliasHelper(column)));
        
        return sql.toString();
    }

    private String aliasHelper (String column) {
        return column.toLowerCase().replace(" ", "_");
    }

    // Helper method for column preparation
    protected String columnPreparation(Object column) {
        return column.toString().contains(" ") ? "\"" + column + "\"" : column.toString();
    }
}