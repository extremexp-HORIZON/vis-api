package gr.imsi.athenarc.xtremexpvisapi.service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;

import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.RawVisDataset;

@Service
public class RawVisDatasetService {
    private static final Logger LOG = LoggerFactory.getLogger(RawVisDatasetService.class);
    
    @Autowired
    private Connection duckdbConnection;

    @Value("${app.working.directory}")
    private String workingDirectory;

    public Optional<RawVisDataset> findById(String id) throws SQLException, IOException {
        Assert.notNull(id, "RawVis dataset id must not be null1");
        ObjectMapper objectMapper = new ObjectMapper();

        RawVisDataset dataset = null;
        // TODO: Change to a more dynamic way to fetch datasets.
        String path =  "/opt/experiments/" + id + "/metadata/" + id + ".meta.json";
        File metadataFile = new File(path);

        if (metadataFile.exists()) {
            LOG.debug("Fetching dataset from: {}", metadataFile);
            FileReader reader = new FileReader(metadataFile);
            dataset = objectMapper.readValue(reader, RawVisDataset.class);
            fillFacets(dataset);
        }

        return Optional.ofNullable(dataset);

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

}
