package gr.imsi.athenarc.xtremexpvisapi.controller;

import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import gr.imsi.athenarc.xtremexpvisapi.domain.Zenoh.AuthenticationRequest;
import gr.imsi.athenarc.xtremexpvisapi.service.FileService;
import gr.imsi.athenarc.xtremexpvisapi.service.ZenohService;

@RestController
@CrossOrigin
@RequestMapping("/zenoh")
public class ZenohController {

    private static final Logger LOG = LoggerFactory.getLogger(ZenohController.class);
    private final ZenohService zenohService;
    private final FileService fileService;

    @Autowired
    public ZenohController(ZenohService zenohService, FileService fileService) {
        this.zenohService = zenohService;
        this.fileService = fileService;
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

    @PostMapping("/getfile")
    public void getFile (@RequestBody String path) {
        try {
            URL url = new URL("https://download.mozilla.org/?product=firefox-stub&os=win&lang=en-GB");
            fileService.downloadFile(url);
        } catch (Exception e) {
            LOG.error("Failed to get file", e);
        }
    }
    
}
