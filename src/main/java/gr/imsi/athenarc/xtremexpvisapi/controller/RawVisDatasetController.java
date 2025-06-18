package gr.imsi.athenarc.xtremexpvisapi.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.RawVisDataset;
import gr.imsi.athenarc.xtremexpvisapi.service.RawVisDatasetService;


@RestController
@CrossOrigin
@RequestMapping("/api")
public class RawVisDatasetController {
    private static final Logger LOG = LoggerFactory.getLogger(RawVisDatasetController.class);

    private final RawVisDatasetService rawVisDatasetService;

    public RawVisDatasetController(RawVisDatasetService rawVisDatasetService) {
        this.rawVisDatasetService = rawVisDatasetService;
    }

    // TODO: Add more endpoints when necessary

    @GetMapping("/datasets/{id}")
    public ResponseEntity<RawVisDataset> getRawVisDataset(@PathVariable("id") String id) throws IOException, SQLException {
        LOG.debug("REST request to get RawVisDataaset : {}", id);
        Optional<RawVisDataset> rOptional = rawVisDatasetService.findById(id);
        LOG.debug(rOptional.toString());
        return rOptional.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
