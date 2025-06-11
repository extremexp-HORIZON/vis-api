package gr.imsi.athenarc.xtremexpvisapi.domain.Query;

import com.google.common.math.PairedStats;

import lombok.Data;

@Data
public class RectStats {

    private long count;
    private Double mean0;
    private Double min0;
    private Double max0;
    private Double variance0;
    private Double standardDeviation0;

    private Double mean1;
    private Double min1;
    private Double max1;
    private Double variance1;
    private Double standardDeviation1;

    private Double pearsonCorrelation;
    private Double covariance;


    public RectStats(PairedStats pairedStats, long count) {

        this.count = count;
        if (count != 0) {
            mean0 = pairedStats.xStats().mean();
            min0 = pairedStats.xStats().min();
            max0 = pairedStats.xStats().max();
            variance0 = pairedStats.xStats().populationVariance();
            standardDeviation0 = pairedStats.xStats().populationStandardDeviation();

            mean1 = pairedStats.yStats().mean();
            min1 = pairedStats.yStats().min();
            max1 = pairedStats.yStats().max();
            variance1 = pairedStats.yStats().populationVariance();
            standardDeviation1 = pairedStats.yStats().populationStandardDeviation();
            covariance = pairedStats.populationCovariance();
            try {
                pearsonCorrelation = pairedStats.pearsonsCorrelationCoefficient();
            } catch (IllegalStateException e) {
                pearsonCorrelation = null;
            }
        }
    }

}
