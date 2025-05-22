package gr.imsi.athenarc.xtremexpvisapi.domain.Metadata;

import java.util.List;
import java.util.Map;

import gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.TabularColumn;
import lombok.Data;

@Data
public class MetadataResponse {

    private DatasetType datasetType;
    private List<String> fileNames;
    private List<TabularColumn> originalColumns;
    private int totalItems;
    private Map<String, List<?>> uniqueColumnValues;
    private boolean hasLatLonColumns;
    private List<String> timeColumn; 

}
