package gr.imsi.athenarc.xtremexpvisapi.domain.datamanagement;

import java.util.List;

import lombok.Data;

@Data
public class CatalogResponse {
    private List<DataItem> data;
    private Integer total;
    private Integer page;
    private Integer perPage;
    private Integer filtered_total;

    @Data
    public static class DataItem {
        private String id;
        private String filename;
        private String upload_filename;
        private String description;
        private List<String> use_case;
        private String path;
        private String user_id;
        private String created;
        private String parent_files;
        private String project_id;
        private String file_size;
        private String file_type;
        private Boolean recdeleted;
    }
}
