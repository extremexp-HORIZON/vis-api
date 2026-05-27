package gr.imsi.athenarc.xtremexpvisapi.domain.lifecycle;

import java.util.Map;
import lombok.Data;

@Data
public class CreateRunRequest {
  private String experimentId;
  private String runName;
  private Map<String, String> params;
}
