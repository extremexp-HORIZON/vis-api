package gr.imsi.athenarc.xtremexpvisapi.service;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gr.imsi.athenarc.xtremexpvisapi.datasource.DataSource;
import gr.imsi.athenarc.xtremexpvisapi.datasource.DataSourceFactory;
import gr.imsi.athenarc.xtremexpvisapi.domain.SourceType;
import gr.imsi.athenarc.xtremexpvisapi.domain.TabularColumn;
import gr.imsi.athenarc.xtremexpvisapi.domain.TabularResults;
import gr.imsi.athenarc.xtremexpvisapi.domain.TimeSeriesResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TabularRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TimeSeriesRequest;

@Service
public class DataService {
    private final DataSourceFactory dataSourceFactory;
    private final Map<String, DataSource> dataSourceCache = new HashMap<>();
     
    @Autowired
    public DataService(DataSourceFactory dataSourceFactory) {
        this.dataSourceFactory = dataSourceFactory;
    }
    private static final Logger LOG = LoggerFactory.getLogger(DataService.class);

    public TabularResults getTabularData(TabularRequest tabularRequest) {
        LOG.info("Retrieving tabular data for datasetId: {}", tabularRequest.getDatasetId());

        String datasetId = tabularRequest.getDatasetId();
        SourceType type = tabularRequest.getType();
        DataSource dataSource = dataSourceCache.computeIfAbsent(datasetId, id -> dataSourceFactory.createDataSource(type, id));
        // Print datasetId being processed
        LOG.info("Processing data for datasetId: {}", datasetId);
        TabularResults results = dataSource.fetchTabularData(tabularRequest);
        return results;
    }
    
    public TimeSeriesResponse getTimeSeriesData(TimeSeriesRequest timeSeriesRequest) {
        LOG.info("Retrieving time series data for datasetId: {}", timeSeriesRequest.getDatasetId());

        String datasetId = timeSeriesRequest.getDatasetId();
        SourceType type = timeSeriesRequest.getType();

        DataSource dataSource = dataSourceCache.computeIfAbsent(datasetId, id -> dataSourceFactory.createDataSource(type, id));
        // Print datasetId being processed
        LOG.info("Processing data for datasetId: {}", datasetId);

        TimeSeriesResponse results = dataSource.fetchTimeSeriesData(timeSeriesRequest);
        return results;
        
    }
    
    public List<TabularColumn> getColumns(String datasetId) {
        LOG.info("Retrieving columns for datasetId: {}", datasetId);
        DataSource dataSource = dataSourceCache.get(datasetId);
        if (dataSource == null) {
            LOG.error("No DataSource found for datasetId: {}", datasetId);
            return null; // or throw an exception
        }
        return dataSource.getColumns();
    }
    
}
