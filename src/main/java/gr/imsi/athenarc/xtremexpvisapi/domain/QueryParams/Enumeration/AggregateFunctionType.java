package gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.Enumeration;

import com.google.common.math.Stats;

public enum AggregateFunctionType {
    AVG, MIN, MAX, SUM, COUNT;

    public static double getAggValue(AggregateFunctionType aggType, Stats stats) {
        if (aggType == null) {
            aggType = COUNT;
        }
        switch (aggType) {
            case AVG:
                return stats.mean();
            case MIN:
                return stats.min();
            case MAX:
                return stats.max();
            case SUM:
                return stats.sum();
            default:
                return stats.count();
        }
    }


}
