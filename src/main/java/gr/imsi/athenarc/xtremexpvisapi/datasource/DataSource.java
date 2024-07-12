

package gr.imsi.athenarc.xtremexpvisapi.datasource;

import java.util.List;

import gr.imsi.athenarc.xtremexpvisapi.domain.VisualColumn;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualizationResults;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualQuery;
import tech.tablesaw.api.Table;

public interface DataSource {
    String getSource();
    VisualizationResults fetchData(VisualQuery visualQuery);
    String getTimestampColumn(Table table);
    String getColumn(String source, String columnName);
    List<VisualColumn> getColumns(String source);
}