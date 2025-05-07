package gr.imsi.athenarc.xtremexpvisapi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.InvalidProtocolBufferException;

import gr.imsi.athenarc.xtremexpvisapi.service.explainability.ExplainabilityService;

@RestController
@CrossOrigin
@RequestMapping("/api/explainability")
public class ExplainabilityController {

    private static final Logger LOG = LoggerFactory.getLogger(ExplainabilityController.class);

    private final ExplainabilityService explainabilityService;

    public ExplainabilityController(ExplainabilityService explainabilityService) {
        this.explainabilityService = explainabilityService;
    }

    @PostMapping("/{experimentId}/{runId}")
    public JsonNode getExplanation(@RequestBody String explainabilityRequest, @PathVariable String experimentId,
            @PathVariable String runId) throws JsonProcessingException, InvalidProtocolBufferException {
        LOG.info("Received explainability request: \n{}", explainabilityRequest);
        return explainabilityService.GetExplains(explainabilityRequest, experimentId, runId);
    }

    @GetMapping("/affected")
    public JsonNode applyAffectedActions() throws JsonProcessingException, InvalidProtocolBufferException {
        LOG.info("Request for apply affected actions");
        return explainabilityService.ApplyAffectedActions();
    }
}