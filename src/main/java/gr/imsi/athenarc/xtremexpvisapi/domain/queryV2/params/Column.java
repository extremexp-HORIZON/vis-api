package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params;

import lombok.Data;

@Data
public class Column {
    
    private String name;
    private String type;

    public Column(String name, String type) {
        this.name = name;
        this.type = type;
    }

}
