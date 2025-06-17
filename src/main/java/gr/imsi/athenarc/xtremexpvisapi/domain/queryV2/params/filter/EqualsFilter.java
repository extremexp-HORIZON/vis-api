package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.filter;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EqualsFilter extends AbstractFilter {
    private Object value;
    
    public EqualsFilter() {
        super();
        setType("equals");
    }
    
    @Override
    public String toSql() {
        if (value == null) {
            return getColumn() + " IS NULL";
        }
        return getColumn() + " = " + escapeSqlValue(value);
    }
}