package gr.imsi.athenarc.xtremexpvisapi.domain.Explainability;

import java.util.Map;

import lombok.Data;

@Data
public class ApplyAffectedActionsRes {
    private Map<String, TableContents> appliedAffectedActions;
}
