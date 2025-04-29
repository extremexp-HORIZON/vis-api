package gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.Filter;

public class StringFilter extends AbstractFilter {

    private String value;
    private String operator; // "contains", "startsWith", "endsWith"

    public StringFilter() {}

    public StringFilter(String column, String value, String operator) {
        super(column);
        this.value = value;
        this.operator = operator;
        setType("string");
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }
}
