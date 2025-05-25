package gr.imsi.athenarc.xtremexpvisapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gr.imsi.athenarc.xtremexpvisapi.datasource.DataSource;
import gr.imsi.athenarc.xtremexpvisapi.datasource.DataSourceFactory;
import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.MetadataRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.MetadataResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TabularRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TabularResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TimeSeriesRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TimeSeriesResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.SourceType;
import tagbio.umap.Umap;

@Service
public class DataService {
    private final DataSourceFactory dataSourceFactory;

    @Autowired
    public DataService(DataSourceFactory dataSourceFactory) {
        this.dataSourceFactory = dataSourceFactory;
    }

    private static final Logger LOG = LoggerFactory.getLogger(DataService.class);

    public TabularResponse getTabularData(TabularRequest tabularRequest) {
        String datasetId = tabularRequest.getDatasetId();
        // datasetId = "I2Cat_phising/dataset/I2Cat_phising_dataset.csv"   ;     
        datasetId = "I2Cat_phising/dataset/moby.csv";

        tabularRequest.setDatasetId(datasetId);
        SourceType type = tabularRequest.getType();
        DataSource dataSource = dataSourceFactory.createDataSource(type, datasetId);

        LOG.info("Processing data for datasetId: {}", datasetId);
        TabularResponse results = dataSource.fetchTabularData(tabularRequest);
        return results;
    }

    public TimeSeriesResponse getTimeSeriesData(TimeSeriesRequest timeSeriesRequest) {
        String datasetId = timeSeriesRequest.getDatasetId();
        SourceType type = timeSeriesRequest.getType();
        DataSource dataSource = dataSourceFactory.createDataSource(type, datasetId);

        LOG.info("Processing data for datasetId: {}", datasetId);
        TimeSeriesResponse results = dataSource.fetchTimeSeriesData(timeSeriesRequest);
        return results;

    }

    public MetadataResponse getFileMetadata(MetadataRequest metadataRequest) {
        LOG.info("Retrieving metadata for datasetId: {}", metadataRequest.getDatasetId());

        String datasetId = metadataRequest.getDatasetId();
        // datasetId = "I2Cat_phising/dataset/I2Cat_phising_dataset.csv";
                datasetId = "I2Cat_phising/dataset/moby.csv";

        metadataRequest.setDatasetId(datasetId);
        SourceType type = metadataRequest.getType();

        DataSource dataSource = dataSourceFactory.createDataSource(type, datasetId);

        return dataSource.getFileMetadata(metadataRequest);
    }

    public float[][] getUmap(float[][] data) {
        LOG.info("Performing dimensionality reduction");
        Umap umap = new Umap();
        umap.setNumberComponents(2);
        umap.setNumberNearestNeighbours(15);
        umap.setThreads(1);
        return umap.fitTransform(data);
    }

    
}
