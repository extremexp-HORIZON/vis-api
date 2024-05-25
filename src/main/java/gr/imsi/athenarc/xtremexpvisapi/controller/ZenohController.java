// package gr.imsi.athenarc.xtremexpvisapi.controller;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.CrossOrigin;
// import org.springframework.web.bind.annotation.GetMapping;

// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.RequestHeader;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

// import gr.imsi.athenarc.xtremexpvisapi.domain.Zenoh.AuthenticationRequest;
// import gr.imsi.athenarc.xtremexpvisapi.service.ZenohService;

// @RestController

// @CrossOrigin
// @RequestMapping("/zenoh")

// public class ZenohController {
//     private static final Logger LOG = LoggerFactory.getLogger(ZenohController.class);
//     private final ZenohService zenohService;

//     public ZenohController(ZenohService zenohService) {
//         this.zenohService = zenohService;
//     }

//     @GetMapping("/files/{useCase}/{folder}")
//     public ResponseEntity<String> listFiles(@PathVariable String useCase, @PathVariable String folder) {
//         try {
//             String fileList = zenohService.listFiles(useCase, folder);
//             return ResponseEntity.ok(fileList);
//         } catch (Exception e) {
//             LOG.error("Failed to list files", e);
//             return ResponseEntity.internalServerError().body("Failed to list files from Zenoh");
//         }
//     }
// }
// // public class ZenohController {

// //     private static final Logger LOG = LoggerFactory.getLogger(ZenohController.class);

// //     private final ZenohService zenohService;
// //     public ZenohController(ZenohService zenohService) {
// //         this.zenohService=zenohService;
        
// //     }

// //     @GetMapping("/fetchZenohData")
// //     public ResponseEntity<String> fetchZenohData(@RequestBody AuthenticationRequest authRequest) {
// //         try {
// //             String token = zenohService.authenticate(authRequest.getUsername(), authRequest.getPassword());
// //             System.out.println("token"+token);
// //             return ResponseEntity.ok(token);  // Assuming you return the token for now
// //         } catch (Exception e) {
// //             e.printStackTrace();
// //             return ResponseEntity.internalServerError().body("Failed to authenticate with Zenoh");
// //         }
// //     }

// //     @GetMapping("/files/{useCase}/{folder}")
// //     public ResponseEntity<String> listFiles(@RequestHeader("Authorization") String token, @PathVariable String useCase, @PathVariable String folder) {
// //         try {
// //             String fileList = zenohService.listFiles(token.replace("Bearer ", ""), useCase, folder);
// //             return ResponseEntity.ok(fileList);
// //         } catch (Exception e) {
// //             LOG.error("Failed to list files", e);
// //             return ResponseEntity.internalServerError().body("Failed to list files from Zenoh");
// //         }
// //     }

    
// // }







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


    @GetMapping("/listFiles")
    public ResponseEntity<?> listFiles() {
        if (accessToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in.");
        }
        try {
            String files = zenohService.listFiles();
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    @GetMapping("/files/{useCase}/{folder}")
    public ResponseEntity<String> CasesFiles(@PathVariable String useCase, @PathVariable String folder) {
        try {
            String fileList = zenohService.CasesFiles(useCase, folder);
            return ResponseEntity.ok(fileList);
        } catch (Exception e) {
            LOG.error("Failed to list files", e);
            return ResponseEntity.internalServerError().body("Failed to list files from Zenoh");
        }
    }

    @GetMapping("/file/{useCase}/{folder}/{subfolder}/{filename}")
    public ResponseEntity<String> getFile(
        @PathVariable String useCase,
        @PathVariable String folder,
        @PathVariable String subfolder,
        @PathVariable String filename) {
        try {
            String fileContent = zenohService.getFile(useCase, folder, subfolder, filename);
            return ResponseEntity.ok().body(fileContent);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }


    @DeleteMapping("/file/{useCase}/{folder}/{subfolder}/{filename}")
    public ResponseEntity<String> deleteFile(
        @PathVariable String useCase,
        @PathVariable String folder,
        @PathVariable String subfolder,
        @PathVariable String filename) {
        try {
            String result = zenohService.deleteFile(useCase, folder, subfolder, filename);
            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
