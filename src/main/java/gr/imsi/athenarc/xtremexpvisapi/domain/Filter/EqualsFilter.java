package gr.imsi.athenarc.xtremexpvisapi.domain.Filter;

import java.time.LocalDateTime;

public class EqualsFilter<T> extends AbstractFilter {

    protected T value;

    public EqualsFilter() {}

    public EqualsFilter(String column, T value) {
        super(column);
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "EqualsFilter [column=" + getColumn() + ", type=" + getType() + ", value=" + value + "]";
    }

    public class NumberEqualsFilter extends EqualsFilter<Double> {

        public NumberEqualsFilter(String column, Double value) {
            super(column, value);
            setType("equals");
        }

        @Override
        public Double getValue() {
            return super.getValue();
        }
    }
        
    public class DateTimeEqualsFilter extends EqualsFilter<LocalDateTime> {

        public DateTimeEqualsFilter(String column, LocalDateTime value) {
            super(column, value);
            setType("equals");
        }

        @Override
        public LocalDateTime getValue() {
            return super.getValue();
        }
    }

    public class StringEqualsFilter extends EqualsFilter<String> {

        public StringEqualsFilter(String column, String value) {
            super(column, value);
            setType("equals");
        }

        @Override
        public String getValue() {
            return super.getValue();
        }
    }
}
