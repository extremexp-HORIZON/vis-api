package gr.imsi.athenarc.xtremexpvisapi.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gr.imsi.athenarc.xtremexpvisapi.datasource.MapQueryExecutor;
import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.RawVisDataset;


@RestController
@CrossOrigin
@RequestMapping("/api")
public class RawVisDatasetController {
    private static final Logger LOG = LoggerFactory.getLogger(RawVisDatasetController.class);

    private final MapQueryExecutor mapQueryExecutor;

    public RawVisDatasetController(MapQueryExecutor mapQueryExecutor) {
        this.mapQueryExecutor = mapQueryExecutor;
    }

    @GetMapping("/datasets/{id}")
    public ResponseEntity<RawVisDataset> getRawVisDataset(@PathVariable("id") String id) throws IOException {
        LOG.debug("REST request to get RawVisDataaset : {}", id);
        Optional<RawVisDataset> rOptional = mapQueryExecutor.fetchDataset(id);
        LOG.debug(rOptional.toString());
        return rOptional.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
