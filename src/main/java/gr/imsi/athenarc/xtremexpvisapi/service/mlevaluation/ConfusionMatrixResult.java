package gr.imsi.athenarc.xtremexpvisapi.service.mlevaluation;

import java.util.List;

public record ConfusionMatrixResult(List<String> labels, List<List<Integer>> matrix) {
}
