package gr.imsi.athenarc.xtremexpvisapi.domain.Filter;

public abstract class AbstractFilter {
 
    private String column;
    private String type;
    private Object value;

    public AbstractFilter() {}

    public AbstractFilter(String column) {
        this.column = column;
    }

    public String getColumn() {
        return column;
    }
    public void setColumn(String column) {
        this.column = column;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "AbstractFilter [column=" + column + ", rawValue=" + value + ", type=" + type + "]";
    }

    
}
