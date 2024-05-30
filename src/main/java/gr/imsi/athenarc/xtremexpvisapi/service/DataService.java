package gr.imsi.athenarc.xtremexpvisapi.service;

import java.util.List;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gr.imsi.athenarc.xtremexpvisapi.datasource.DataSource;
import gr.imsi.athenarc.xtremexpvisapi.datasource.DataSourceFactory;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualColumn;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualizationResults;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualQuery;

@Service
public class DataService {

    private static final Logger LOG = LoggerFactory.getLogger(DataService.class);
    

    public VisualizationResults getData(VisualQuery visualQuery) {
        LOG.info("Retrieving columns for datasetId: {}", visualQuery.getDatasetId());

        String datasetId = visualQuery.getDatasetId();

        DataSource dataSource = DataSourceFactory.createDataSource("csv", datasetId);
        // Print datasetId being processed
        LOG.info("Processing data for datasetId: {}", datasetId);
        
        return dataSource.fetchData(visualQuery);
    }

    public List<VisualColumn> getColumns(String datasetId) {
        LOG.info("Retrieving columns for datasetId: {}", datasetId);
        DataSource dataSource = DataSourceFactory.createDataSource("csv", datasetId);
        return dataSource.getColumns(datasetId);
    }

    public String getColumn(String datasetId, String columnName) {
        LOG.info("Retrieving column {} for datasetId: {}", columnName, datasetId);
        DataSource dataSource = DataSourceFactory.createDataSource("csv", datasetId);
        return dataSource.getColumn(datasetId, columnName);
    } 
}
