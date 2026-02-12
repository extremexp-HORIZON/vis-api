package gr.imsi.athenarc.xtremexpvisapi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.InvalidProtocolBufferException;

import gr.imsi.athenarc.xtremexpvisapi.service.explainability.ExplainabilityService;
import gr.imsi.athenarc.xtremexpvisapi.service.experiment.ExperimentServiceFactory;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.Run;

import java.util.Collections;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/api/explainability")
public class ExplainabilityController {

    private static final Logger LOG = LoggerFactory.getLogger(ExplainabilityController.class);

    private final ExplainabilityService explainabilityService;
    private final ExperimentServiceFactory experimentServiceFactory;
    private final ObjectMapper objectMapper;

    public ExplainabilityController(ExplainabilityService explainabilityService,
                                    ExperimentServiceFactory experimentServiceFactory,
                                    ObjectMapper objectMapper) {
        this.explainabilityService = explainabilityService;
        this.experimentServiceFactory = experimentServiceFactory;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/{experimentId}/{runId}")
    public JsonNode getExplanation(@RequestBody String explainabilityRequest, @PathVariable String experimentId,
            @PathVariable String runId, @RequestHeader(value = "Authorization", required = false) String authorization)
            throws JsonProcessingException, InvalidProtocolBufferException {
        LOG.info("Received explainability request: \n{}", explainabilityRequest);

        return explainabilityService.GetExplains(explainabilityRequest, experimentId, runId, authorization);
    }

    @PostMapping("/{experimentId}/{runId}/feature-importance")
    public JsonNode getFeatureImportance(@RequestBody String featureImportanceRequest,@PathVariable String experimentId,
            @PathVariable String runId, @RequestHeader(value="Authorization", required=false) String authorization)
            throws JsonProcessingException, InvalidProtocolBufferException {
        // LOG.info("Received feature importance request:\n{}", featureImportanceRequest);
                LOG.info("Received feature importance request");

        return explainabilityService.getFeatureImportance(featureImportanceRequest,experimentId, runId, authorization);
    }

    @GetMapping("/affected")
    public JsonNode applyAffectedActions() throws JsonProcessingException, InvalidProtocolBufferException {
        LOG.info("Received request for affected ppl");
        return explainabilityService.ApplyAffectedActions();
    }

    // Convenience endpoint: backend fetches runs for the experiment and forwards
    // them to the explainability gRPC service, so clients don't need to
    // manually copy/paste JSON.
    @PostMapping("/{experimentId}/experiment-highlights")
    public JsonNode runExperimentHighlightsForExperiment(@PathVariable String experimentId)
            throws JsonProcessingException, InvalidProtocolBufferException {

        LOG.info("Received experiment highlights request for experiment {}", experimentId);

        ResponseEntity<List<Run>> response =
                experimentServiceFactory.getActiveService().getRunsForExperiment(experimentId);

        List<Run> runs = response.getBody();
        if (runs == null) {
            runs = Collections.emptyList();
        }

        String runsJson = objectMapper.writeValueAsString(runs);
        return explainabilityService.runExperimentHighlights(runsJson);
    }

}