package gr.imsi.athenarc.xtremexpvisapi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import gr.imsi.athenarc.xtremexpvisapi.domain.Zenoh.AuthenticationRequest;
import gr.imsi.athenarc.xtremexpvisapi.service.ZenohService;

@RestController
@CrossOrigin
@RequestMapping("/zenoh")
public class ZenohController {

    private static final Logger LOG = LoggerFactory.getLogger(ZenohController.class);
    private final ZenohService zenohService;

    public ZenohController(ZenohService zenohService) {
        this.zenohService = zenohService;
    }
    private String accessToken = null; // You might want to manage the token more securely or refresh it as needed


    @PostMapping("/authenticate")
    public ResponseEntity<String> authenticate(@RequestBody AuthenticationRequest authRequest) {
        try {
            String token = zenohService.authenticate(authRequest.getUsername(), authRequest.getPassword());
            accessToken=token;
            return ResponseEntity.ok(token);  // Returns the access token for now
        } catch (Exception e) {
            LOG.error("Failed to authenticate with Zenoh", e);
            return ResponseEntity.internalServerError().body("Failed to authenticate with Zenoh");
        }
    }

    @GetMapping("/files/{useCase}/{folder}/{subfolder}/{filename}")
    public ResponseEntity<String> CasesFiles(@PathVariable String useCase, @PathVariable String folder, @PathVariable String subfolder,@PathVariable String filename ) {
        try {
            String fileList = zenohService.CasesFiles(useCase, folder, subfolder,filename);
            LOG.info("filelist"+fileList);
            return ResponseEntity.ok(fileList);
        } catch (Exception e) {
            LOG.error("Failed to list files", e);
            return ResponseEntity.internalServerError().body("Failed to list files from Zenoh");
        }
    }
    
}
