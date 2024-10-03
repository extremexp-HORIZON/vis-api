



package gr.imsi.athenarc.xtremexpvisapi.service;

import java.util.List;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gr.imsi.athenarc.xtremexpvisapi.datasource.DataSource;
import gr.imsi.athenarc.xtremexpvisapi.datasource.DataSourceFactory;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualColumn;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualizationDataRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualizationResults;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualQuery;

@Service
public class DataService {
    private final ZenohService zenohService;
    private final DataSourceFactory dataSourceFactory;    // private final DataSourceFactory dataSourceFactory;

    @Autowired
    public DataService(ZenohService zenohService, DataSourceFactory dataSourceFactory) {
        this.zenohService = zenohService;
        this.dataSourceFactory = dataSourceFactory;
    }
    private static final Logger LOG = LoggerFactory.getLogger(DataService.class);

    public VisualQuery queryPreperation (VisualizationDataRequest visualizationDataRequest) {

        VisualQuery visualQuery = new VisualQuery(
            visualizationDataRequest.getDatasetId(),
            visualizationDataRequest.getViewPort(), 
            visualizationDataRequest.getColumns(),
            visualizationDataRequest.getLimit(),
            visualizationDataRequest.getScaler(),
            visualizationDataRequest.getAggFunction(),
            visualizationDataRequest.getOffset()
        );
        switch (visualizationDataRequest.getVisualizationType()) {
            case "tabular":
                // Specific handling for tabular data
                break;
            case "temporal":
                visualQuery.setTemporalParams(
                    visualizationDataRequest.getTemporalParams().getGroupColumn(),
                    visualizationDataRequest.getTemporalParams().getGranularity()
                );
                break;
            case "geographical":
                visualQuery.setGeographicalParams(
                    visualizationDataRequest.getGeographicalParams().getLat(),
                    visualizationDataRequest.getGeographicalParams().getLon()
                );
                break;
            default:
                throw new IllegalArgumentException("Unsupported visualization type: " + visualizationDataRequest.getVisualizationType());
        }


        if(!visualizationDataRequest.getDatasetId().endsWith(".json")){
            visualQuery.instantiateFilters(
            visualizationDataRequest.getFilters(),
            getColumns(visualizationDataRequest.getDatasetId())
        );
        }
        return visualQuery;
    }
    
    

    public VisualizationResults getData(VisualQuery visualQuery) {
        LOG.info("Retrieving columns for datasetId: {}", visualQuery.getDatasetId());

        String datasetId = visualQuery.getDatasetId();
        String type = datasetId.startsWith("file://") ? "csv" : "zenoh";


        DataSource dataSource = dataSourceFactory.createDataSource(type, datasetId);
        // Print datasetId being processed
        LOG.info("Processing data for datasetId: {}", datasetId);
        VisualizationResults results = dataSource.fetchData(visualQuery);
        return results;
    }

    public List<VisualColumn> getColumns(String datasetId) {
        LOG.info("Retrieving columns for datasetId: {}", datasetId);
        String type = datasetId.startsWith("file://") ? "csv" : "zenoh";

        DataSource dataSource = dataSourceFactory.createDataSource(type, datasetId);
        return dataSource.getColumns(datasetId);
    }

    public String getColumn(String datasetId, String columnName) {
        LOG.info("Retrieving column {} for datasetId: {}", columnName, datasetId);
        DataSource dataSource = dataSourceFactory.createDataSource("csv", datasetId);
        return dataSource.getColumn(datasetId, columnName);
    } 

    public String fetchZenohData(String useCase, String folder, String subfolder, String filename) throws Exception {
        return zenohService.CasesFiles(useCase, folder, subfolder, filename);
    }
}
