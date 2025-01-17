package gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.Filter;

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
    @JsonSubTypes.Type(value = RangeFilter.class, name = "range")
})
public abstract class AbstractFilter {
 
    private String column;
    private String type;

    public AbstractFilter() {}

    public AbstractFilter(String column) {
        this.column = column;
    }
    
}
