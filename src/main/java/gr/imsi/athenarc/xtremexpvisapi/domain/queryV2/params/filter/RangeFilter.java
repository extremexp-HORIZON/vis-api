package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.filter;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RangeFilter extends AbstractFilter {
    private Object min;
    private Object max;
    
    public RangeFilter() {
        super();
        setType("range");
    }
    
    @Override
    public String toSql() {
        if (min != null && max != null) {
            return columnPreparation(getColumn()) + " BETWEEN " + escapeSqlValue(min) + " AND " + escapeSqlValue(max);
        } else if (min != null) {
            return columnPreparation(getColumn()) + " >= " + escapeSqlValue(min);
        } else if (max != null) {
            return columnPreparation(getColumn()) + " <= " + escapeSqlValue(max);
        }
        return "1=1"; // No filter
    }
}