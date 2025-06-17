package gr.imsi.athenarc.xtremexpvisapi.datasource;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.davidmoten.geo.Coverage;
import com.github.davidmoten.geo.GeoHash;
import com.github.davidmoten.geo.LatLong;
import com.google.common.collect.Range;

import gr.athenarc.imsi.visualfacts.Veti;
import gr.athenarc.imsi.visualfacts.Schema;
import gr.athenarc.imsi.visualfacts.Rectangle;
import gr.athenarc.imsi.visualfacts.CategoricalColumn;
import gr.athenarc.imsi.visualfacts.query.Query;
import gr.athenarc.imsi.visualfacts.query.QueryResults;
import static gr.athenarc.imsi.visualfacts.config.IndexConfig.DELIMITER;

import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.RawVisDataset;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.GroupedStats;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.MapDataRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.MapDataResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.RectStats;
import gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.Enumeration.AggregateFunctionType;
import tech.tablesaw.api.Table;

@Component
public class MapQueryExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(MapQueryExecutor.class);
    private HashMap<String, Veti> indexes = new HashMap<>();

    public void removeIndex(String datasetId) {
        indexes.remove(datasetId);
    }

    public Optional<RawVisDataset> fetchDataset(String id) throws IOException {
        Assert.notNull(id, "RawVis dataset id must not be null1");
        ObjectMapper objectMapper = new ObjectMapper();

        RawVisDataset dataset = null;
        String path =  "/opt/experiments/" + id + "/metadata/" + id + ".meta.json";
        File metadataFile = new File(path);

        if (metadataFile.exists()) {
            FileReader reader = new FileReader(metadataFile);
            dataset = objectMapper.readValue(reader, RawVisDataset.class);
        }

        return Optional.ofNullable(dataset);
    }

    private Veti initIndex(Path path, Table table, RawVisDataset dataset) {
        // TODO mapping from mapDataRequest or Query/VisQuery from rawvis-index to 0-based index of csv's columns and pass to Schema constructor
        Rectangle rectangle = new Rectangle(Range.open(dataset.getXMin(), dataset.getXMax()), Range.open(dataset.getYMin(), dataset.getYMax()));
        
        LOG.info("Initializing new Visualfacts.Schema for dataset id: {}", dataset.getId());
        Schema schema = new Schema(path.toString(), DELIMITER, table.columnIndex("longitude"), 
                            table.columnIndex("latitude"), table.columnIndex(dataset.getMeasure0()), 
                            table.columnIndex(dataset.getMeasure1()), rectangle, dataset.getObjectCount());
        schema.setHasHeader(dataset.getHasHeader());
        List<CategoricalColumn> categoricalColumns = dataset.getDimensions().stream().map(field -> new CategoricalColumn(table.columnIndex(field))).collect(Collectors.toList());
        schema.setCategoricalColumns(categoricalColumns);
        LOG.debug(schema.toString());
        Veti veti = new Veti(schema, 100000000, "binn", 100);
        this.indexes.put(dataset.getId(), veti);
        return veti;
    }

    public Integer getObjectsIndexed(String id) {
        Veti index = indexes.get(id);
        return index == null ? 0 : index.getObjectsIndexed();
    }

    public boolean isIndexInitialized(String id) {
        Veti index = indexes.get(id);
        return index != null && index.isInitialized();
    }

    public MapDataResponse queryMapData(Path path, Table table, MapDataRequest mapDataRequest) {
        MapDataResponse mapDataResponse = new MapDataResponse();
        try {
            
            RawVisDataset dataset = fetchDataset(mapDataRequest.getDatasetId()).get();
            List<Integer> groupByColsIndexes = mapDataRequest.getGroupByCols().stream().map(col -> table.columnIndex(col)).toList();
            Integer measureColIndex = table.columnIndex(mapDataRequest.getMeasureCol());
            Map<Integer, String> categoricalFilters = mapDataRequest.getCategoricalFilters().entrySet().stream()
                .collect(Collectors.toMap(entry -> table.columnIndex(entry.getKey()), Map.Entry::getValue));
            Query query = new Query(new Rectangle(mapDataRequest.getRectangle().getXRange(), mapDataRequest.getRectangle().getYRange()), categoricalFilters, groupByColsIndexes, measureColIndex);
            LOG.info(dataset.toString());
            
            LOG.info(query.toString());
            Veti veti = indexes.get(mapDataRequest.getDatasetId());
            if (veti == null) {
                veti = this.initIndex(path, table, dataset);
                veti.executeQuery(query);
            }
            Schema schema = veti.getSchema();
            QueryResults queryResults = veti.executeQuery(query);

            // LOG.debug(queryResults.toString());
            mapDataResponse.setFullyContainedTileCount(queryResults.getFullyContainedTileCount());
            mapDataResponse.setIoCount(queryResults.getIoCount());
            mapDataResponse.setPointCount(queryResults.getPoints().size());
            mapDataResponse.setTileCount(queryResults.getTileCount());
            mapDataResponse.setTotalPointCount(veti.getObjectsIndexed());
            mapDataResponse.setTotalTileCount(veti.getLeafTileCount());

            if (queryResults.getRectStats() != null) {
                mapDataResponse.setRectStats(new RectStats(queryResults.getRectStats().snapshot()));
            }

            mapDataResponse.setSeries((queryResults.getStats().entrySet().stream().map(e ->
                new GroupedStats(e.getKey(), AggregateFunctionType.getAggValue(mapDataRequest.getAggType(),
                    mapDataRequest.getMeasureCol().equals(table.column(schema.getMeasureCol0()).name()) ? e.getValue().xStats() : e.getValue().yStats())))).collect(Collectors.toList()));
            
            List<Object[]> points;
            if (queryResults.getPoints() != null) {
                points = queryResults.getPoints();
            } else {
                points = new ArrayList<>();
            }

            // Process points using GeoHash similar to SQLDataService
            Map<String, List<Object[]>> geoHashGroups = new HashMap<>();
            
            // Group points by GeoHash
            Coverage coverage = GeoHash.coverBoundingBoxMaxHashes(
                mapDataRequest.getRectangle().getYRange().upperEndpoint(), 
                mapDataRequest.getRectangle().getXRange().lowerEndpoint(), 
                mapDataRequest.getRectangle().getYRange().lowerEndpoint(), 
                mapDataRequest.getRectangle().getXRange().upperEndpoint(), 
                10000);
                
            int hashLength = coverage.getHashLength();
            
            // Group points by geohash
            for (Object[] point : points) {
                if (point.length < 2) continue;
                
                // Get lat, lon from point
                float lat = (float) point[0];
                float lon = (float) point[1];
                
                // Point size/count is at position 2
                Integer pointCount = point.length > 2 ? (Integer) point[2] : 1;

                // Extract values based on the point format from Veti
                Object id = point.length > 3 ? point[3] : null;
                Float measure0 = point.length > 4 ? (Float) point[4] : null;
                Float measure1 = point.length > 5 ? (Float) point[5] : null;
                List<String> groupByValues = point.length > 6 ? (List<String>) point[6] : null;
                
                String geoHashValue = GeoHash.encodeHash(lat, lon, hashLength);
                
                // Store original point data with geohash, including point count
                Object[] pointData = new Object[] { lat, lon, pointCount, id, measure0, measure1, groupByValues };
                geoHashGroups.computeIfAbsent(geoHashValue, k -> new ArrayList<>()).add(pointData);
            }
            
            // Process geohash groups into aggregated points
            List<Object[]> processedPoints = new ArrayList<>();
            
            for (Map.Entry<String, List<Object[]>> entry : geoHashGroups.entrySet()) {
                String geoHash = entry.getKey();
                List<Object[]> groupedPoints = entry.getValue();
                LatLong latLong = GeoHash.decodeHash(geoHash);
                
                // If only one point in the group, retain its original values
                if (groupedPoints.size() == 1) {
                    Object[] singlePoint = groupedPoints.get(0);
                    processedPoints.add(new Object[] { 
                        (float) latLong.getLat(), 
                        (float) latLong.getLon(), 
                        singlePoint[2],  // point count
                        singlePoint[3],  // id
                        singlePoint[4],  // measure0
                        singlePoint[5],  // measure1
                        singlePoint[6]   // groupByValues
                    });
                } else {
                    // Sum up the point counts
                    int totalPointCount = groupedPoints.stream()
                        .mapToInt(p -> p[2] != null ? (Integer)p[2] : 1)
                        .sum();
                    
                    // Calculate weighted average measures for multiple points
                    double measure0Sum = 0;
                    double measure1Sum = 0;
                    int measure0Count = 0;
                    int measure1Count = 0;
                    
                    for (Object[] p : groupedPoints) {
                        Integer count = (Integer) p[2];
                        if (p[4] != null) {
                            measure0Sum += ((Float)p[4]) * count;
                            measure0Count += count;
                        }
                        if (p[5] != null) {
                            measure1Sum += ((Float)p[5]) * count;
                            measure1Count += count;
                        }
                    }
                    
                    Float measure0Avg = measure0Count > 0 ? (float)(measure0Sum / measure0Count) : null;
                    Float measure1Avg = measure1Count > 0 ? (float)(measure1Sum / measure1Count) : null;
                    
                    // Process grouped categorical/groupBy values
                    List<List<String>> groupedGroupByValues = processGroupByValues(groupedPoints);
                    
                    processedPoints.add(new Object[] { 
                        (float) latLong.getLat(), 
                        (float) latLong.getLon(), 
                        totalPointCount,  // total count of all points in this group
                        null,  // no single ID for multiple points
                        measure0Avg, 
                        measure1Avg,
                        groupedGroupByValues
                    });
                }
            }
            
            mapDataResponse.setPoints(processedPoints);
            mapDataResponse.setFacets(schema.getCategoricalColumns().stream().collect(Collectors.toMap(col -> table.column(col.getIndex()).name(), CategoricalColumn::getNonNullValues)));

            // LOG.debug(mapDataResponse.toString());
            return mapDataResponse;
        } catch (IOException e) {
            e.printStackTrace();
            throw new UncheckedIOException(e);
        }
    }
    
    // Helper method to process group by values from multiple points
    private List<List<String>> processGroupByValues(List<Object[]> points) {
        // List to store processed group values
        List<List<String>> result = new ArrayList<>();
        
        // Check if we have any valid group by values
        boolean hasGroupValues = false;
        for (Object[] point : points) {
            if (point[5] != null && point[5] instanceof List) {
                hasGroupValues = true;
                break;
            }
        }
        
        if (!hasGroupValues) {
            return result;
        }
        
        // Collect all group values
        List<List<String>> allGroupValues = new ArrayList<>();
        for (Object[] point : points) {
            if (point[5] != null && point[5] instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> groupValues = (List<String>) point[5];
                allGroupValues.add(groupValues);
            }
        }
        
        // Ensure there's at least one row with group values
        if (allGroupValues.isEmpty()) {
            return result;
        }
        
        // Find unique values for each column
        int columnCount = allGroupValues.get(0).size();
        for (int i = 0; i < columnCount; i++) {
            Set<String> uniqueValues = new java.util.HashSet<>();
            
            for (List<String> groupValues : allGroupValues) {
                if (i < groupValues.size() && groupValues.get(i) != null) {
                    uniqueValues.add(groupValues.get(i));
                }
            }
            
            // Add the unique values for this column
            result.add(new ArrayList<>(uniqueValues));
        }
        
        return result;
    }
}

