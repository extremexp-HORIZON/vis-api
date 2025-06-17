package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.filter;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

@Data
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = EqualsFilter.class, name = "equals"),
    @JsonSubTypes.Type(value = RangeFilter.class, name = "range"),
    @JsonSubTypes.Type(value = InequalityFilter.class, name = "inequality"),
    @JsonSubTypes.Type(value = StringFilter.class, name = "string")
})
public abstract class AbstractFilter {
 
    private String column;
    private String type;

    public AbstractFilter() {}

    public AbstractFilter(String column) {
        this.column = column;
    }
    
    // Abstract method for SQL generation
    public abstract String toSql();
    
    // Helper method for SQL injection protection
    protected String escapeSqlValue(Object value) {
        if (value == null) return "NULL";
        if (value instanceof String) {
            return "'" + value.toString().replace("'", "''") + "'";
        }
        return value.toString();
    }
}