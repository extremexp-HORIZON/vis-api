package gr.imsi.athenarc.xtremexpvisapi.datasource;

import tech.tablesaw.api.Table;

public class QueryResult {
    private Table resultTable;
    private int rowCount;

    public QueryResult(Table resultTable, int rowCount) {
        this.resultTable = resultTable;
        this.rowCount = rowCount;
    }

    public Table getResultTable() {
        return resultTable;
    }

    public int getRowCount() {
        return rowCount;
    }
}
