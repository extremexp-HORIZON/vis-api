package gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.Filter;

public class RangeFilter<T> extends AbstractFilter {

    protected T min;
    protected T max;

    public RangeFilter() {}
    
    public RangeFilter(String column, T min, T max) {
        super(column);
        this.min = min;
        this.max = max;
    }

    public T getMin() {
        return min;
    }

    public void setMin(T min) {
        this.min = min;
    }

    public T getMax() {
        return max;
    }

    public void setMax(T max) {
        this.max = max;
    }
}
