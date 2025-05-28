package gr.imsi.athenarc.xtremexpvisapi.domain.DataManagement;

import java.util.List;

import lombok.Data;

@Data
public class CatalogResponse {
    private List<Object> data;
    private Integer total;
    private Integer page;
    private Integer perPage;
    private Integer filtered_total;
}
