package gr.imsi.athenarc.xtremexpvisapi.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import gr.imsi.athenarc.xtremexpvisapi.datasource.QueryExecutor;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualColumn;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualizationResults;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualQuery;

@Service
public class DataService {

    private static final Logger LOG = LoggerFactory.getLogger(DataService.class);
    @CacheEvict(value = "datasetCache", allEntries = true)
    public void clearCache() {
        // This method can be called to clear the cache
    }
    
    @Value("${app.schema.path}")
    String schemaPath = "";
    
    public static List<Double> stringToDoubleList(String valuesString) {
        String[] values = valuesString.substring(2, valuesString.length() - 2).split(", ");

         List<Double> doubleList = new ArrayList<>();

        // Convert each string element to a double and add to the list
        for (String value : values) {
            doubleList.add(Double.parseDouble(value));
        }

        return doubleList;
    }

    @Cacheable("datasetCache")

    public VisualizationResults getData(VisualQuery visualQuery) {
        LOG.info("Retrieving columns for datasetId: {}", visualQuery.getDatasetId());

        VisualizationResults visualizationResults = new VisualizationResults();
        String datasetId = visualQuery.getDatasetId();

        // Print datasetId being processed
        LOG.info("Processing data for datasetId: {}", datasetId);

        if (visualQuery.getFilters().contains(null)) {
            visualizationResults.setMessage("500");
            LOG.warn("Null filter detected in visualQuery");
            return visualizationResults;
        }
        
        QueryExecutor queryExecutor = new QueryExecutor(datasetId, Path.of(schemaPath, datasetId + ".csv").toString());
    
        // Print executing query with visualQuery
        LOG.info("Executing.. query for datasetId: {}", visualQuery.toString());
        return queryExecutor.executeQuery(visualQuery);
    }

    public List<VisualColumn> getColumns(String datasetId) {
        LOG.info("Retrieving columns for datasetId: {}", datasetId);
        QueryExecutor queryExecutor = new QueryExecutor(datasetId, Path.of(schemaPath, datasetId + ".csv").toString());
        return queryExecutor.getColumns(datasetId);
    }

    public String getColumn(String datasetId, String columnName) {
        LOG.info("Retrieving column {} for datasetId: {}", columnName, datasetId);
        QueryExecutor queryExecutor = new QueryExecutor(datasetId, Path.of(schemaPath, datasetId + ".csv").toString());
        return queryExecutor.getColumn(datasetId, columnName);
    } 
}
