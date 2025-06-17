package gr.imsi.athenarc.xtremexpvisapi.controller;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gr.imsi.athenarc.xtremexpvisapi.domain.DataManagement.CatalogRequest;
import gr.imsi.athenarc.xtremexpvisapi.service.DataManagementService;
import lombok.extern.log4j.Log4j2;

@RestController
@CrossOrigin
@Log4j2
@RequestMapping("/data-management")
public class DataManagementController {

    private final DataManagementService dataManagementService;

    @Autowired
    public DataManagementController(DataManagementService dataManagementService) {
        this.dataManagementService = dataManagementService;
    }
    
    @GetMapping("/get-catalog")
    public CompletableFuture<ResponseEntity<?>> getZenohCatalog(@RequestBody CatalogRequest catalogRequest) throws Exception {
        log.info("Received request for getting Zenoh catalog with search params: {}", catalogRequest.toString());
        return dataManagementService.getCatalogInfo(catalogRequest)
                .<ResponseEntity<?>>thenApply(catalogResponse -> {
                    log.info("Successfully retrieved Zenoh catalog");
                    return ResponseEntity.ok(catalogResponse);
                })
                .exceptionally(e -> {
                    log.error("Error retrieving Zenoh catalog", e);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Error retrieving Zenoh catalog: " + e.getMessage());
                });
    }
}
