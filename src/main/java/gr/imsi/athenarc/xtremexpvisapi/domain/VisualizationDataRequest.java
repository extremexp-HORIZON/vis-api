package gr.imsi.athenarc.xtremexpvisapi.domain;

import java.util.List;

import gr.imsi.athenarc.xtremexpvisapi.domain.DataExplorationAdditionalParams.GeographicalParams;
import gr.imsi.athenarc.xtremexpvisapi.domain.DataExplorationAdditionalParams.TabularParams;
import gr.imsi.athenarc.xtremexpvisapi.domain.DataExplorationAdditionalParams.TemporalParams;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.VisualFilter;
import lombok.Data;


@Data
public class VisualizationDataRequest {

    private String datasetId;
    private ViewPort viewPort;
    private List<String> columns;
    private List<VisualFilter> filters;
    private String aggFunction;
    private Integer limit;
    private String scaler;
    private String visualizationType;
    private String visualizationMethod;
    private TemporalParams temporalParams;
    private GeographicalParams geographicalParams;
    private TabularParams tabularParams;
    private Integer offset; // New field for offset

}
