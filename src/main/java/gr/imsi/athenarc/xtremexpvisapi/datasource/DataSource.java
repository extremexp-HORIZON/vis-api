

package gr.imsi.athenarc.xtremexpvisapi.datasource;

import java.util.List;


import gr.imsi.athenarc.xtremexpvisapi.domain.TabularResults;
import gr.imsi.athenarc.xtremexpvisapi.domain.TimeSeriesResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualColumn;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualizationResults;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TabularQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TimeSeriesQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualQuery;

public interface DataSource {
    String getSource();
    VisualizationResults fetchData(VisualQuery visualQuery);
    String getTimestampColumn();
    String getColumn(String columnName);
    List<VisualColumn> getColumns();
    TabularResults fetchTabularData(TabularQuery tabularQuery);
    TimeSeriesResponse fetchTimeSeriesData(TimeSeriesQuery timeSeriesQuery);
}
