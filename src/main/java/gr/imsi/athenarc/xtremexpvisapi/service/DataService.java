package gr.imsi.athenarc.xtremexpvisapi.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.checkerframework.checker.units.qual.s;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gr.imsi.athenarc.xtremexpvisapi.datasource.DataSource;
import gr.imsi.athenarc.xtremexpvisapi.datasource.DataSourceFactory;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualColumn;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualizationResults;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualQuery;
import tagbio.umap.Umap;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.FloatColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

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

    public float[][] getUMapData() {
        try (InputStream inputStream = new FileInputStream("/opt/xxp/msi.csv")) {
            CsvReadOptions csvReadOptions = CsvReadOptions.builder(inputStream).build();
            Table msi = Table.read().usingOptions(csvReadOptions);
            LOG.info(msi.structure().toString());
            
            int rowCount = msi.rowCount();
            int columnCount = msi.columnCount();
            float[][] dataArray = new float[rowCount][columnCount];
            
            for (int i = 0; i < rowCount; i++) {
                Row row = msi.row(i);
                for (int j = 0; j < columnCount; j++) {
                    if (row.getColumnType(j) == ColumnType.INTEGER) {
                        dataArray[i][j] = (float) row.getInt(j);
                    } else if (row.getColumnType(j) == ColumnType.DOUBLE) {
                        dataArray[i][j] = (float) row.getDouble(j);
                    } else if (row.getColumnType(j) == ColumnType.FLOAT) {
                        dataArray[i][j] = row.getFloat(j);
                    }
                }
            }
            final Umap umap = new Umap();
            umap.setNumberComponents(2);
            umap.setNumberNearestNeighbours(15);
            umap.setThreads(1); 
            return umap.fitTransform(dataArray);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read CSV from file", e);
        }
    }
}
