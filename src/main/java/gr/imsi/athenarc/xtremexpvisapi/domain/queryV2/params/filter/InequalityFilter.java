package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.filter;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class InequalityFilter extends AbstractFilter {
    private Object value;
    private String operator; // "gt", "lt", "gte", "lte", "ne"
    
    public InequalityFilter() {
        super();
        setType("inequality");
    }
    
    @Override
    public String toSql() {
        if (value == null || operator == null) {
            return "1=1";
        }
        
        String sqlOperator;
        switch (operator.toLowerCase()) {
            case "gt": sqlOperator = ">"; break;
            case "lt": sqlOperator = "<"; break;
            case "gte": sqlOperator = ">="; break;
            case "lte": sqlOperator = "<="; break;
            case "ne": sqlOperator = "!="; break;
            default: return "1=1";
        }
        
        return columnPreparation(getColumn()) + " " + sqlOperator + " " + escapeSqlValue(value);
    }
}