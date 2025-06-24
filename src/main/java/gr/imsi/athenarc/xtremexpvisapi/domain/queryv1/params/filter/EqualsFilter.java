package gr.imsi.athenarc.xtremexpvisapi.domain.queryv1.params.filter;

public class EqualsFilter<T> extends AbstractFilter {

    protected T value;

    public EqualsFilter() {}

    public EqualsFilter(String column, T value) {
        super(column);
        this.value = value;
        setType("equals");  // You can set type here for consistency

    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

}
