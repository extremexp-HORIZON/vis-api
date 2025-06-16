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

            Map<String, Float> geoHashes = new HashMap<>();

            Coverage coverage = GeoHash.coverBoundingBoxMaxHashes(mapDataRequest.getRectangle().getYRange().upperEndpoint(), mapDataRequest.getRectangle().getXRange().lowerEndpoint(), mapDataRequest.getRectangle().getYRange().lowerEndpoint(), mapDataRequest.getRectangle().getXRange().upperEndpoint(), 10000);
            points.stream().forEach(point -> {
                String geoHashValue = GeoHash.encodeHash((float)point[0], (float)point[1], coverage.getHashLength());
                geoHashes.merge(geoHashValue, 1f, Float::sum);
            });
            points = geoHashes.entrySet().stream().map(e -> {
                LatLong latLong = GeoHash.decodeHash(e.getKey());
                return new Object[]{(float) latLong.getLat(), (float) latLong.getLon(), e.getValue()};
            }).collect(Collectors.toList());

            mapDataResponse.setPoints(points);
            mapDataResponse.setFacets(schema.getCategoricalColumns().stream().collect(Collectors.toMap(col -> table.column(col.getIndex()).name(), CategoricalColumn::getNonNullValues)));

            // LOG.debug(mapDataResponse.toString());
            return mapDataResponse;
        } catch (IOException e) {
            e.printStackTrace();
            throw new UncheckedIOException(e);
        }
    }
}

