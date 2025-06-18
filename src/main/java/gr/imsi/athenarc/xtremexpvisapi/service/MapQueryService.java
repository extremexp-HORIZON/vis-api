package gr.imsi.athenarc.xtremexpvisapi.service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.github.davidmoten.geo.GeoHash;
import com.github.davidmoten.geo.LatLong;
import com.google.common.math.PairedStatsAccumulator;
import com.google.common.math.StatsAccumulator;

import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.RawVisDataset;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.GroupedStats;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.MapDataRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.MapDataResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.RectStats;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TimeSeriesQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.UnivariateDataPoint;
import lombok.extern.java.Log;

@Service
@Log
public class MapQueryService {
    
    @Autowired
    private Connection duckdbConnection;

    @Value("${app.working.directory}")
    private String workingDirectory;
    
    private static String timestampCol = "radio_timestamp";

    @Async
    public CompletableFuture<MapDataResponse> executeMapDataRequest(MapDataRequest mapDataRequest, RawVisDataset rawVisDataset) throws SQLException, Exception {
        return buildQuery(mapDataRequest, rawVisDataset).thenCompose(sql -> {
            try {
                log.info(sql);
                Statement statement = duckdbConnection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql);                

                MapDataResponse mapDataResponse = new MapDataResponse();

                processResults(resultSet, mapDataRequest, rawVisDataset, mapDataResponse, 9);

                // Close resources
                resultSet.close();
                statement.close();
                log.info("Finished process Results");

                return CompletableFuture.completedFuture(mapDataResponse);            
            } catch (Exception e) {
                CompletableFuture<MapDataResponse> failedFuture = new CompletableFuture<>();
                failedFuture.completeExceptionally(e);
                return failedFuture;

            }
        });
    }

    @Async
    public CompletableFuture<List<UnivariateDataPoint>> executeTimesSeriesQuery(RawVisDataset rawVisDataset, TimeSeriesQuery timeSeriesQuery) throws Exception {
        List<UnivariateDataPoint> timeSeriesPoints = new ArrayList<>();
        try {
            String csvPath = workingDirectory + String.format("%1$s/dataset/%1$s.csv", rawVisDataset.getId());

            long intervalSeconds = timeSeriesQuery.getFrequency();
            String sql = String.format(
                "SELECT floor(extract(epoch from %1$s)/(%2$s)) as time_bucket, avg(%3$s) as average_value " +
                "FROM read_csv('%4$s') " +
                "WHERE %1$s BETWEEN '%5$s' AND '%6$s' " +
                "AND %7$s BETWEEN '%8$s' AND '%9$s' " +
                "AND %10$s BETWEEN '%11$s' AND '%12$s' ",
                timestampCol,
                intervalSeconds,
                timeSeriesQuery.getMeasure(),
                csvPath,
                new Timestamp(timeSeriesQuery.getFrom()),
                new Timestamp(timeSeriesQuery.getTo()),
                rawVisDataset.getLat(),
                timeSeriesQuery.getRectangle().getLat().lowerEndpoint(),
                timeSeriesQuery.getRectangle().getLat().upperEndpoint(),
                rawVisDataset.getLon(),
                timeSeriesQuery.getRectangle().getLon().lowerEndpoint(),
                timeSeriesQuery.getRectangle().getLon().upperEndpoint()
            );

            StringBuilder filterBuilder = new StringBuilder();
            if (timeSeriesQuery.getCategoricalFilters() != null && !timeSeriesQuery.getCategoricalFilters().isEmpty()) {
                timeSeriesQuery.getCategoricalFilters().forEach((key, value) -> {
                    filterBuilder.append(String.format(" AND %s = '%s'", key, value));
                });
                sql += filterBuilder.toString();
            }

            sql += " GROUP BY time_bucket ORDER BY time_bucket";

            log.info(sql);
            Statement statement = duckdbConnection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);

            while (resultSet.next()) {
                long bucketEpoch = resultSet.getLong("time_bucket") * intervalSeconds; 
                Double avgValue = resultSet.getObject("average_value") != null ? resultSet.getDouble("average_value") : null;
                Timestamp timestamp = new Timestamp(bucketEpoch * 1000);
                if(avgValue != null) {
                    timeSeriesPoints.add(new UnivariateDataPoint(timestamp.getTime(), avgValue));
                }
            }

            return CompletableFuture.completedFuture(timeSeriesPoints);
        } catch (Exception e) {
            CompletableFuture<List<UnivariateDataPoint>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        }
    }

    @Async
    public CompletableFuture<String> buildQuery(MapDataRequest mapDataRequest, RawVisDataset rawVisDataset) throws Exception {
        try {
            String csvPath = workingDirectory + String.format("%1$s/dataset/%1$s.csv", mapDataRequest.getDatasetId());
            StringBuilder sql = new StringBuilder();

            String latCol = rawVisDataset.getLat();
            String lonCol = rawVisDataset.getLon();

            // SELECT clause
            sql.append("SELECT ");
            sql.append(String.join(", ", "id", latCol, lonCol, 
                rawVisDataset.getMeasure0(), rawVisDataset.getMeasure1(), String.join(", ", rawVisDataset.getDimensions())));

            // FROM clause (later add switch for more input types)
            sql.append(" FROM ");
            sql.append("read_csv('").append(csvPath).append("')");

            // WHERE clause
            sql.append(" WHERE ");
            sql.append(latCol + " BETWEEN " + mapDataRequest.getRect().getLat().lowerEndpoint() + " AND " + mapDataRequest.getRect().getLat().upperEndpoint());
            sql.append(" AND " + lonCol + " BETWEEN " + mapDataRequest.getRect().getLon().lowerEndpoint() + " AND " + mapDataRequest.getRect().getLon().upperEndpoint());

            if (mapDataRequest.getCategoricalFilters() != null && !mapDataRequest.getCategoricalFilters().isEmpty()) {
                sql.append(mapDataRequest.getCategoricalFilters().entrySet().stream()
                            .map(entry -> " AND " + entry.getKey() + " = " + entry.getValue())
                            .collect(Collectors.joining()));
            }

            if (mapDataRequest.getFrom() != null && mapDataRequest.getTo() != null) {
                sql.append(" AND " + timestampCol + " BETWEEN '" + new Timestamp(mapDataRequest.getFrom()) + "' AND '" + new Timestamp(mapDataRequest.getTo()) + "'");
            } else if (mapDataRequest.getFrom() != null) {
                sql.append(" AND " + timestampCol + " >= '" + new Timestamp(mapDataRequest.getFrom()) + "'");
            } else if (mapDataRequest.getTo() != null) {
                sql.append(" AND " + timestampCol + " <= '" + new Timestamp(mapDataRequest.getTo()) + "'");
            }


            return CompletableFuture.completedFuture(sql.toString());
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException();
        }
    }

    /**
     * Function to process the SQL results just like RawVis
     * @param resultSet
     * @param dataRequest
     * @param rawVisDataset
     * @param mapDataResponse
     * @param geohashLength
     * @throws SQLException
     */
    private void processResults(ResultSet resultSet, MapDataRequest mapDataRequest, RawVisDataset rawVisDataset, MapDataResponse mapDataResponse, int geohashLength) throws SQLException {

        Map<String, List<Object[]>> geohashGroups = new HashMap<>();
        Map<String, StatsAccumulator> groupStatsMap = new HashMap<>();
        PairedStatsAccumulator pairedStatsAccumulator = new PairedStatsAccumulator();
        int rawPointCount = 0; // Initialize a counter for raw data points

        while (resultSet.next()) {
            rawPointCount++;

            double lat = resultSet.getDouble(rawVisDataset.getLat());
            double lon = resultSet.getDouble(rawVisDataset.getLon());
            String id = resultSet.getString("id");

            // get measures and handle nulls
            Double measure0 = resultSet.getObject(rawVisDataset.getMeasure0()) != null ? resultSet.getDouble(rawVisDataset.getMeasure0()) : null;
            Double measure1 = resultSet.getObject(rawVisDataset.getMeasure1()) != null ? resultSet.getDouble(rawVisDataset.getMeasure1()) : null;
            String geohash = GeoHash.encodeHash(lat, lon, geohashLength);
            List<String> groupValues = new ArrayList<>();
            for (String colName : mapDataRequest.getGroupByCols()) {
                groupValues.add(resultSet.getString(colName));
            }
            List<String> categoricalValues = new ArrayList<>();
            for (String colName : rawVisDataset.getDimensions()) {
                categoricalValues.add(resultSet.getString(colName));
            }
            Object[] point = new Object[] { lat, lon, id, measure0, measure1, categoricalValues};
            geohashGroups.computeIfAbsent(geohash, k -> new ArrayList<>()).add(point);

            String groupKey = String.join(",", groupValues);

            Double measureValue = resultSet.getObject(mapDataRequest.getMeasureCol()) != null ? resultSet.getDouble(mapDataRequest.getMeasureCol()) : null;
            if(measureValue != null)
                groupStatsMap.computeIfAbsent(groupKey, k -> new StatsAccumulator()).add(measureValue);

            // For paired statistical analysis
            if(measure0 != null && measure1 != null)
                pairedStatsAccumulator.add(measure0, measure1);

        }
        log.info("Raw Point count: " + rawPointCount);

        List<Object[]> points = new ArrayList<>();
        for (Map.Entry<String, List<Object[]>> entry : geohashGroups.entrySet()) {
            List<Object[]> groupedPoints = entry.getValue();
            String geohash = entry.getKey();
            LatLong geohashCenter = GeoHash.decodeHash(geohash);

            if (groupedPoints.size() == 1) {
                Object[] singlePoint = groupedPoints.get(0);
                points.add(new Object[] { geohashCenter.getLat(), geohashCenter.getLon(), 1, singlePoint[2], singlePoint[3], singlePoint[4], singlePoint[5]});
            } else {
                OptionalDouble measure0AvgOpt = groupedPoints.stream().map(point -> point[3]).filter(value -> value != null).mapToDouble(value -> (double) value).average();
                OptionalDouble measure1AvgOpt = groupedPoints.stream().map(point -> point[4]).filter(value -> value != null).mapToDouble(value -> (double) value).average();

                Double measure0Avg = measure0AvgOpt.isPresent() ? measure0AvgOpt.getAsDouble() : null;
                Double measure1Avg = measure1AvgOpt.isPresent() ? measure1AvgOpt.getAsDouble() : null;
                // Process groupedPoints using streams
                @SuppressWarnings("unchecked")
                List<List<String>> groupedGroupByValues = processStrings(groupedPoints.stream()
                    .map(allPoints -> (List<String>) allPoints[5])
                    .collect(Collectors.toList()));
            
                points.add(new Object[] { geohashCenter.getLat(), geohashCenter.getLon(), groupedPoints.size(), null, measure0Avg, measure1Avg, groupedGroupByValues});
            }
        }

        List<GroupedStats> series = groupStatsMap.entrySet().stream()
                .map(entry -> {
                    String key = entry.getKey();
                    StatsAccumulator accumulator = entry.getValue();
                    List<String> group = Arrays.asList(key.split(","));
                    double value = 0;
                    switch (mapDataRequest.getAggType()) {
                        case AVG:
                            value = accumulator.mean();
                            break;
                        case SUM:
                            value = accumulator.sum();
                            break;
                        case MIN:
                            value = accumulator.min();
                            break;
                        case MAX:
                            value = accumulator.max();
                            break;
                        case COUNT:
                            value = accumulator.count();
                            break;
                        default:
                            break;
                    }
                    return new GroupedStats(group, value);
                })
                .collect(Collectors.toList());

        mapDataResponse.setSeries(series);
        mapDataResponse.setFacets(rawVisDataset.getFacets());
        mapDataResponse.setPoints(points);
        mapDataResponse.setPointCount(rawPointCount);
        mapDataResponse.setRectStats(new RectStats(pairedStatsAccumulator.snapshot()));
    }

    private List<List<String>> processStrings(List<List<String>> listOfLists) {
        // List to store the list of unique strings for each column
        List<List<String>> processedStrings = new ArrayList<>();

        // Ensure there's at least one row to avoid IndexOutOfBoundsException
        if (listOfLists.isEmpty()) return processedStrings;

        // Iterate over each column
        for (int i = 0; i < listOfLists.get(0).size(); i++) {
            Set<String> uniqueStrings = new HashSet<>();

            // Collect all unique strings in the current column
            for (int j = 0; j < listOfLists.size(); j++) {
                String currentString = listOfLists.get(j).get(i);
                if (currentString != null) {
                    uniqueStrings.add(currentString);
                }
            }

            // Convert the set of unique strings to a list and add it to the results
            processedStrings.add(new ArrayList<>(uniqueStrings));
        }

        return processedStrings;
    }
    
}
