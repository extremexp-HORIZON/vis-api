package gr.imsi.athenarc.xtremexpvisapi.domain.ExplabilityProcedure;

import java.util.List;

public class TableContents {
    private List<String> values;

    // Getters
    public List<String> getValues() {
        return values;
    }

    // Setters
    public void setValues(List<String> values) {
        this.values = values;
    }

    @Override
    public String toString() {
        return "TableContents{" +
                "values=" + values +
                '}';
    }
}