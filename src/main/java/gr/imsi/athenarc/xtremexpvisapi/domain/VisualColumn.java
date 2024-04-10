package gr.imsi.athenarc.xtremexpvisapi.domain;

public class VisualColumn {
    
    private String name;
    private String type;
    
    public VisualColumn(String name, String type) {
        this.name = name;
        this.type = type;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    @Override
    public String toString() {
        return "Field [name=" + name + ", type=" + type + "]";
    }

    

}
