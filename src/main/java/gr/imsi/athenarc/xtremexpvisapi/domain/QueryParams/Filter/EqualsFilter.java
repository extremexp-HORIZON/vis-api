package gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.Filter;

import java.time.LocalDateTime;

public class EqualsFilter<T> extends AbstractFilter {

    protected T equalValue;

    public EqualsFilter() {}

    public EqualsFilter(String column, T equalValue) {
        super(column);
        this.equalValue = equalValue;
        setType("equals");  // You can set type here for consistency

    }

    public T getEqualValue() {
        return equalValue;
    }

    public void setEqualValue(T equalValue) {
        this.equalValue = equalValue;
    }

    @Override
    public String toString() {
        return "EqualsFilter [column=" + getColumn() + ", type=" + getType() + ", equalValue=" + equalValue + "]";
    }

    public class DoubleEqualsFilter extends EqualsFilter<Double> {

        public DoubleEqualsFilter(String column, Double value) {
            super(column, value);
            setType("equals");
        }

        @Override
        public Double getValue() {
            return super.getEqualValue();
        }
    }

    public class IntegerEqualsFilter extends EqualsFilter<Integer> {

        public IntegerEqualsFilter(String column, Integer value) {
            super(column, value);
            setType("equals");
        }

        @Override
        public Integer getValue() {
            return super.getEqualValue();
        }
    }
        
    public class DateTimeEqualsFilter extends EqualsFilter<LocalDateTime> {

        public DateTimeEqualsFilter(String column, LocalDateTime value) {
            super(column, value);
            setType("equals");
        }

        @Override
        public LocalDateTime getValue() {
            return super.getEqualValue();
        }
    }

    public class StringEqualsFilter extends EqualsFilter<String> {

        public StringEqualsFilter(String column, String value) {
            super(column, value);
        }

        
    }
}
