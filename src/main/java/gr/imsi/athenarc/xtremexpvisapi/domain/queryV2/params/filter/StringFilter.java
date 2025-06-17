package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.filter;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StringFilter extends AbstractFilter {
    private String value;
    private String operation = "contains"; // "contains", "startsWith", "endsWith", "equals"
    private boolean caseSensitive = false;
    
    public StringFilter() {
        super();
        setType("string");
    }
    
    @Override
    public String toSql() {
        if (value == null || value.isEmpty()) {
            return "1=1";
        }
        
        String column = caseSensitive ? getColumn() : "LOWER(" + getColumn() + ")";
        String val = caseSensitive ? value : value.toLowerCase();
        val = val.replace("'", "''"); // Escape single quotes
        
        switch (operation.toLowerCase()) {
            case "contains":
                return column + " LIKE '%" + val + "%'";
            case "startswith":
                return column + " LIKE '" + val + "%'";
            case "endswith":
                return column + " LIKE '%" + val + "'";
            case "equals":
                return column + " = '" + val + "'";
            default:
                return column + " LIKE '%" + val + "%'";
        }
    }
}