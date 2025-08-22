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

    private String prepareColumn(String col) {
    if (col == null) return "col";
    // Add quotes if name has non-alphanumeric or underscores
    return col.matches("[a-zA-Z_][a-zA-Z0-9_]*") ? col : "\"" + col.replace("\"", "\"\"") + "\"";
}

private String sanitizeAlias(String col) {
    if (col == null) return "col";
    // Replace anything not alphanumeric or underscore with underscore
    return col.toLowerCase().replaceAll("[^a-zA-Z0-9_]", "_");
}

    // Generate SQL for this aggregation
    public String toSql() {
    StringBuilder sql = new StringBuilder();
    String preparedColumn = prepareColumn(column);  // 1. Properly quoted

    switch (function) {
        case COUNT:
            if (options != null && options.isDistinct()) {
                sql.append("COUNT(DISTINCT ").append(preparedColumn).append(")");
            } else {
                sql.append("COUNT(").append(preparedColumn).append(")");
            }
            break;

        case COUNT_ALL:
            sql.append("COUNT(*)");
            break;

        case PERCENTILE:
            double percentile = (options != null && options.getPercentileValue() != null)
                ? options.getPercentileValue()
                : 0.5;
            sql.append("PERCENTILE_CONT(").append(percentile)
                .append(") WITHIN GROUP (ORDER BY ").append(preparedColumn).append(")");
            break;

        case ARRAY_AGG:
            if (options != null && options.isDistinct()) {
                sql.append("ARRAY_AGG(DISTINCT ").append(preparedColumn).append(")");
            } else {
                sql.append("ARRAY_AGG(").append(preparedColumn).append(")");
            }
            break;

        default:
            if (options != null && options.isDistinct()) {
                sql.append(function.name()).append("(DISTINCT ").append(preparedColumn).append(")");
            } else {
                sql.append(function.name()).append("(").append(preparedColumn).append(")");
            }
            break;
    }

    // Use custom alias if provided, otherwise generate safe alias
    String aliasName = (alias != null)
        ? alias
        : (function == AggregationFunction.COUNT_ALL
            ? "count_all"
            : function.name().toLowerCase() + "_" + sanitizeAlias(column));

    sql.append(" AS ").append(sanitizeAlias(aliasName));  // 2. Alias must be safe
    return sql.toString();
}


    private String aliasHelper (String column) {
        // Handle special characters in column names for aliases
        if (column == null) return "col";
        return column.toLowerCase().replace(" ", "_").replace("*", "all");
    }

    // Helper method for column preparation
    protected String columnPreparation(Object column) {
        return column.toString().contains(" ") ? "\"" + column + "\"" : column.toString();
    }
}