package gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.Filter;

import lombok.Data;

@Data
public abstract class AbstractFilter {
 
    private String column;
    private String type;
    private Object value;

    public AbstractFilter() {}

    public AbstractFilter(String column) {
        this.column = column;
    }
    
}
