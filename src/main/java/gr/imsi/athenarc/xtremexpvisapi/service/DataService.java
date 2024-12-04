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
import gr.imsi.athenarc.xtremexpvisapi.domain.SOURCE_TYPE;
import gr.imsi.athenarc.xtremexpvisapi.domain.TabularColumn;
import gr.imsi.athenarc.xtremexpvisapi.domain.TabularRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.TabularResults;
import gr.imsi.athenarc.xtremexpvisapi.domain.TimeSeriesRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.TimeSeriesResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TabularQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TimeSeriesQuery;

@Service
public class DataService {
    private final DataSourceFactory dataSourceFactory;
    private final Map<String, DataSource> dataSourceCache = new HashMap<>();
     
    @Autowired
    public DataService(DataSourceFactory dataSourceFactory) {
        this.dataSourceFactory = dataSourceFactory;
    }
    private static final Logger LOG = LoggerFactory.getLogger(DataService.class);

    public TabularQuery tabularQueryPreperation (TabularRequest tabularRequest) {

        TabularQuery tabularQuery = new TabularQuery(
            tabularRequest.getDatasetId(),
            tabularRequest.getLimit(),
            tabularRequest.getColumns(),
            tabularRequest.getOffset(),
            tabularRequest.getGroupBy(),
            tabularRequest.getAggregation(),
            tabularRequest.getType()
        );
        
        if(!tabularQuery.getDatasetId().endsWith(".json")){
            tabularQuery.instantiateFilters(
            tabularRequest.getFilters(),
            getColumns(tabularRequest.getDatasetId())
        );
        }
        return tabularQuery;
    }
    

    public TimeSeriesQuery timeSeriesQueryPreperation(TimeSeriesRequest timeSeriesRequest) {
        TimeSeriesQuery timeSeriesQuery = new TimeSeriesQuery(
            timeSeriesRequest.getDatasetId(),
            timeSeriesRequest.getTimestampColumn(),
            timeSeriesRequest.getColumns(),
            timeSeriesRequest.getFrom(),
            timeSeriesRequest.getTo(),
            timeSeriesRequest.getLimit(),
            timeSeriesRequest.getOffset(),
            timeSeriesRequest.getDataReduction(),
            timeSeriesRequest.getType()
        );
        if(!timeSeriesQuery.getDatasetId().endsWith(".json")){
            timeSeriesQuery.instantiateFilters();
        }
        return timeSeriesQuery;
        
    }
    public TabularResults getTabularData(TabularQuery tabularQuery) {
        LOG.info("Retrieving tabcolumns for datasetId: {}", tabularQuery.getDatasetId());

        String datasetId = tabularQuery.getDatasetId();
        SOURCE_TYPE type = tabularQuery.getType();
        DataSource dataSource = dataSourceCache.computeIfAbsent(datasetId, id -> dataSourceFactory.createDataSource(type, id));
        // Print datasetId being processed
        LOG.info("Processing data for datasetId: {}", datasetId);
        TabularResults results = dataSource.fetchTabularData(tabularQuery);
        return results;
    }
    
    public TimeSeriesResponse getTimeSeriesData(TimeSeriesQuery timeSeriesQuery) {
        LOG.info("Retrieving tabcolumns for datasetId: {}", timeSeriesQuery.getDatasetId());

        String datasetId = timeSeriesQuery.getDatasetId();
        SOURCE_TYPE type = timeSeriesQuery.getType();

        DataSource dataSource = dataSourceCache.computeIfAbsent(datasetId, id -> dataSourceFactory.createDataSource(type, id));
        // Print datasetId being processed
        LOG.info("Processing data for datasetId: {}", datasetId);

        TimeSeriesResponse results = dataSource.fetchTimeSeriesData(timeSeriesQuery);
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
