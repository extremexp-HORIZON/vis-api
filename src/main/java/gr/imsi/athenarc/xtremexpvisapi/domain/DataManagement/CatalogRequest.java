package gr.imsi.athenarc.xtremexpvisapi.domain.datamanagement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class CatalogRequest {
    private String filename;
    private String use_case;
    private List<String> project_id;
    private String created_from;
    private String created_to;
    private String useId;
    private String file_type;
    private String parent_files;
    private String size_from;
    private String size_to;
    private String sort = "id.asc";
    private String page = "1";
    private String perPage = "1000";

    public CatalogRequest(List<String> project_id, String filename) {
        this.project_id = project_id;
        this.filename = filename;
    }

    public Map<String, String> toMap() {
        Map<String, String> params = new HashMap<>();

        if (filename != null)
            params.put("filename", filename);
        if (use_case != null)
            params.put("use_case", use_case);
        if (project_id != null && !project_id.isEmpty())
            params.put("project_id", String.join(",", project_id));
        if (created_from != null)
            params.put("created_from", created_from);
        if (created_to != null)
            params.put("created_to", created_to);
        if (useId != null)
            params.put("useId", useId);
        if (file_type != null)
            params.put("file_type", file_type);
        if (parent_files != null)
            params.put("parent_files", parent_files);
        if (size_from != null)
            params.put("size_from", size_from);
        if (size_to != null)
            params.put("size_to", size_to);

        // Always include sort, page, and perPage (they have default values)
        params.put("sort", sort);
        params.put("page", page);
        params.put("perPage", perPage);

        return params;
    }

}
