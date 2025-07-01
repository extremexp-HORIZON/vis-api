package gr.imsi.athenarc.xtremexpvisapi.datasource;

import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.MetadataRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.MetadataResponseV1;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv1.TabularRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv1.TabularResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv1.TimeSeriesRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv1.TimeSeriesResponse;

@Deprecated
public interface DataSource {
    String getSource();

    MetadataResponseV1 getFileMetadata(MetadataRequest metadataRequest);

    TabularResponse fetchTabularData(TabularRequest tabularRequest);

    TimeSeriesResponse fetchTimeSeriesData(TimeSeriesRequest timeSeriesRequest);
}
