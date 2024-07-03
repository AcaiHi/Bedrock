// package com.amazon.aws.controller;

// import com.amazon.aws.service.FileProcessingService;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestParam;
// import org.springframework.web.bind.annotation.RestController;
// import org.springframework.web.multipart.MultipartFile;

// @RestController
// public class FileUploadController {

//     @Autowired
//     private FileProcessingService fileProcessingService;

//     @PostMapping("/api/upload")
//     public String handleFileUpload(@RequestParam("wordFile") MultipartFile wordFile,
//                                    @RequestParam("excelFile") MultipartFile excelFile) {
//         try {
//             return fileProcessingService.processFiles(wordFile, excelFile);
//         } catch (Exception e) {
//             return "Failed to read files: " + e.getMessage();
//         }
//     }
// }
package com.amazon.aws.controller;

import com.amazon.aws.service.FileProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class FileUploadController {

    @Autowired
    private FileProcessingService fileProcessingService;

    @PostMapping("/api/upload")
    public String handleFileUpload(@RequestParam("wordFile") MultipartFile wordFile,
                                   @RequestParam("excelFile") MultipartFile excelFile) {
        try {
            return fileProcessingService.processFiles(wordFile, excelFile);
        } catch (Exception e) {
            return "Failed to read files: " + e.getMessage();
        }
    }
}
