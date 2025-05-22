package gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.Filter;

public class InequalityFilter<T> extends AbstractFilter {

    protected T value;
    protected String operator; // "gt", "lt", "gte", "lte"

    public InequalityFilter() {}

    public InequalityFilter(String column, T value, String operator) {
        super(column);
        this.value = value;
        this.operator = operator;
        setType("inequality");
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }
}
