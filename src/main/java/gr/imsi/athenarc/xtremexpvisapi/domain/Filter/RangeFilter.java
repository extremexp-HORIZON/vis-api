package gr.imsi.athenarc.xtremexpvisapi.domain.Filter;

import java.time.LocalDateTime;

public class RangeFilter<T> extends AbstractFilter {

    protected T minValue;
    protected T maxValue;

    public RangeFilter() {}
    public RangeFilter(String column, T minValue, T maxValue) {
        super(column);
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public T getMinValue() {
        return minValue;
    }

    public void setMinValue(T minValue) {
        this.minValue = minValue;
    }

    public T getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(T maxValue) {
        this.maxValue = maxValue;
    }

    @Override
    public String toString() {
        return "RangeFilter [column=" + getColumn() + ", type=" + getType() + ", minValue=" + minValue + ", maxValue=" + maxValue + "]";
    }

    public class NumberRangeFilter extends RangeFilter<Double> {

        public NumberRangeFilter(String column, Double minValue, Double maxValue) {
            super(column, minValue, maxValue);
            setType("range");
        }

        @Override
        public Double getMaxValue() {
            return super.getMaxValue();
        }

        @Override
        public Double getMinValue() {
            return super.getMinValue();
        }
    }
        
    public class DateTimeRangeFilter extends RangeFilter<LocalDateTime> {

        public DateTimeRangeFilter(String column, LocalDateTime minValue, LocalDateTime maxValue) {
            super(column, minValue, maxValue);
            setType("range");
        }

        @Override
        public LocalDateTime getMaxValue() {
            return super.getMaxValue();
        }

        @Override
        public LocalDateTime getMinValue() {
            return super.getMinValue();
        }
    }
}
