package gr.imsi.athenarc.xtremexpvisapi.datasource;

import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.MetadataRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.MetadataResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TabularRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TabularResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TimeSeriesRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TimeSeriesResponse;

public interface DataSource {
    String getSource();
    MetadataResponse getFileMetadata(MetadataRequest metadataRequest);
    TabularResponse fetchTabularData(TabularRequest tabularRequest);
    TimeSeriesResponse fetchTimeSeriesData(TimeSeriesRequest timeSeriesRequest);
}
