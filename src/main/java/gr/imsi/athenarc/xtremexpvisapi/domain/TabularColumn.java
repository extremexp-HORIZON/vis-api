package gr.imsi.athenarc.xtremexpvisapi.domain;

import lombok.Data;

@Data
public class TabularColumn {
    
    private String name;
    private String type;

    public TabularColumn(String name, String type) {
        this.name = name;
        this.type = type;
    }

}
