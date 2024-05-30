package gr.imsi.athenarc.xtremexpvisapi.domain.ExplabilityProcedure;

import java.util.List;

public class TableContents {
    private int index;
    private List<String> values;

    // Getters
    public int getIndex() {
        return index;
    }

    public List<String> getValues() {
        return values;
    }

    // Setters
    public void setIndex(int index) {
        this.index = index;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    @Override
    public String toString() {
        return "TableContents{" +
                "index=" + index +
                ", values=" + values +
                '}';
    }
}