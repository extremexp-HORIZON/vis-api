

package gr.imsi.athenarc.xtremexpvisapi.datasource;

import java.util.List;

import gr.imsi.athenarc.xtremexpvisapi.domain.TabularColumn;
import gr.imsi.athenarc.xtremexpvisapi.domain.TabularResults;
import gr.imsi.athenarc.xtremexpvisapi.domain.TimeSeriesResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TabularRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TimeSeriesRequest;

public interface DataSource {
    String getSource();
    List<TabularColumn> getColumns();
    TabularResults fetchTabularData(TabularRequest tabularRequest);
    TimeSeriesResponse fetchTimeSeriesData(TimeSeriesRequest timeSeriesRequest);
}
