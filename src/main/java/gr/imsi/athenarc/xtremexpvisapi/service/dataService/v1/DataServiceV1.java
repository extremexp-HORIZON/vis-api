package gr.imsi.athenarc.xtremexpvisapi.service.dataService.v1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gr.imsi.athenarc.xtremexpvisapi.datasource.DataSource;
import gr.imsi.athenarc.xtremexpvisapi.datasource.DataSourceFactory;
import gr.imsi.athenarc.xtremexpvisapi.domain.metadata.MetadataRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.metadata.MetadataResponseV1;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv1.TabularRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv1.TabularResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv1.TimeSeriesRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv1.TimeSeriesResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv1.params.SourceType;
import tagbio.umap.Umap;

@Service
@Deprecated
public class DataServiceV1 {
    private final DataSourceFactory dataSourceFactory;

    @Autowired
    public DataServiceV1(DataSourceFactory dataSourceFactory) {
        this.dataSourceFactory = dataSourceFactory;
    }

    private static final Logger LOG = LoggerFactory.getLogger(DataServiceV1.class);

    public TabularResponse getTabularData(TabularRequest tabularRequest) {
        String datasetId = tabularRequest.getDatasetId();


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

    public MetadataResponseV1 getFileMetadata(MetadataRequest metadataRequest) {
        LOG.info("Retrieving metadata for datasetId: {}", metadataRequest.getDatasetId());

        String datasetId = metadataRequest.getDatasetId();
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
