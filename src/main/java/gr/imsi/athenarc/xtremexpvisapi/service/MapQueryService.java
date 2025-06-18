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

import gr.imsi.athenarc.xtremexpvisapi.datasource.MapQueryExecutor;
import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.RawVisDataset;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.GroupedStats;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.MapDataRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.MapDataResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.RectStats;
import lombok.extern.java.Log;

@Service
@Log
public class MapQueryService {
    
    @Autowired
    private Connection duckdbConnection;

    @Value("${app.working.directory}")
    private String workingDirectory;

    private final MapQueryExecutor mapQueryExecutor;
    
    private static String timestampCol = "radio_timestamp";

    public MapQueryService(MapQueryExecutor mapQueryExecutor) {
        this.mapQueryExecutor = mapQueryExecutor;
    }

    @Async
    public CompletableFuture<MapDataResponse> executeMapDataRequest(MapDataRequest mapDataRequest) throws SQLException, Exception {
        return buildQuery(mapDataRequest).thenCompose(sql -> {
            try {
                log.info(sql);
                Statement statement = duckdbConnection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql);
                
                /** Uncomment the lines below to log the resultSet */
                // int columnCount = resultSet.getMetaData().getColumnCount();
                // StringBuilder sb = new StringBuilder();
                // int totalCount = 0;
                // while (resultSet.next()) {
                //     totalCount++;
                //     for (int i = 1; i <= columnCount; i++) {
                //         sb.append(resultSet.getMetaData().getColumnName(i)).append(": ")
                //           .append(resultSet.getString(i)).append(" ");
                //     }
                //     sb.append(System.lineSeparator());
                // }
                // // log.info("ResultSet:\n" + sb.toString());
                // log.info("Total count: " + totalCount);
                // resultSet.beforeFirst();
                

                MapDataResponse mapDataResponse = new MapDataResponse();
                // TODO Move fetchDataset to a properService that the DataController.java calls.
                RawVisDataset dataset = mapQueryExecutor.fetchDataset(mapDataRequest.getDatasetId()).get();
                fillFacets(dataset);

                processResults(resultSet, mapDataRequest, dataset, mapDataResponse, 9);

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
    public CompletableFuture<String> buildQuery(MapDataRequest mapDataRequest) throws Exception {
        // Fetch RawVisDataset from meta.json
        try {
            RawVisDataset dataset = mapQueryExecutor.fetchDataset(mapDataRequest.getDatasetId()).get();
            String csvPath = workingDirectory + String.format("%1$s/dataset/%1$s.csv", mapDataRequest.getDatasetId());
            StringBuilder sql = new StringBuilder();

            String latCol = dataset.getLat();
            String lonCol = dataset.getLon();

            // SELECT clause
            sql.append("SELECT ");
            sql.append(String.join(", ", "id", latCol, lonCol, 
                dataset.getMeasure0(), dataset.getMeasure1(), String.join(", ", dataset.getDimensions())));

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

    private void fillFacets(RawVisDataset rawVisDataset) throws SQLException{
        try {
            String csvPath = workingDirectory + String.format("%1$s/dataset/%1$s.csv", rawVisDataset.getId());
            Map<String, List<String>> facets = new HashMap<>();
            for (String dimension : rawVisDataset.getDimensions()) {
                String sql = String.format(
                    "SELECT DISTINCT %s FROM read_csv('%s') LIMIT 10",
                    dimension,
                    csvPath
                );
                Statement statement = duckdbConnection.createStatement();
                ResultSet rs = statement.executeQuery(sql);
                StringBuilder sb = new StringBuilder();
                List<String> values = new ArrayList<>();
                while (rs.next()) {
                    sb.append(dimension).append(": ")
                      .append(rs.getString(1)).append(System.lineSeparator());
                    values.add(rs.getString(1));
                }
                // log.info("Facet counts for " + dimension + ":\n" + sb.toString());
                facets.put(dimension, values);
                rs.close();
                statement.close();
            }
            rawVisDataset.setFacets(facets);
        } catch (SQLException e) {
            throw e;
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
        // TODO: Check why RectStats are not counted properly.
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
