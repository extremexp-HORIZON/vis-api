package gr.imsi.athenarc.xtremexpvisapi.domain.queryv2.params.filter;

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
            return columnPreparation(getColumn()) + " IS NULL";
        }
        return columnPreparation(getColumn()) + " = " + escapeSqlValue(value);
    }
}