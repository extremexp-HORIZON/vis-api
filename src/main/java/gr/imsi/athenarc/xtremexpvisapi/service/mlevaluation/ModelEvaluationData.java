package gr.imsi.athenarc.xtremexpvisapi.service.mlevaluation;

import tech.tablesaw.api.Table;

public record ModelEvaluationData(Table xTest, Table yTest, Table yPred, Table xTrain, Table yTrain) {
}
