
package org.download.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.download.exception.InvalidFileException;
import org.download.model.TagRating;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.download.exception.StorageFileNotFoundException;
import org.download.services.StorageService;

@RestController
@CrossOrigin("*")
public class FileUploadController {

    @Autowired
    private RestTemplate restTemplate;
    private final StorageService storageService;
    @Autowired
    public FileUploadController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/")
    public String listUploadedFiles(Model model) throws IOException {

        model.addAttribute("files",
                storageService.loadAll().map(
                                path -> MvcUriComponentsBuilder.
                                        fromMethodName(FileUploadController.class,
                                                "serveFile", path.
                                                        getFileName().
                                                        toString()).build().toUri().toString())
                        .collect(Collectors.toList()));

        return "uploadForm";
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> handleFileUpload(@RequestParam("file") MultipartFile file,
                                                                RedirectAttributes redirectAttributes) {
        Map<String, Object> response = storageService.handleFileUpload(file);

        Object statusObject = response.get("status");
        HttpStatus status;

        if(statusObject != null && statusObject.equals("success")) {
            status = HttpStatus.OK;
        } else if (statusObject != null && statusObject.equals("error")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, (String) response.get("message"));
        } else {
            status = HttpStatus.BAD_REQUEST;
        }
        return new ResponseEntity<>(response, status);
    }

    @Async
    @PostMapping("/send")
    public ResponseEntity<String> sendFileToOtherService(@RequestParam("fileName") String fileName) {
        return storageService.sendFileToOtherService(fileName);
    }

    @GetMapping("/getTagsAndScores")
    @ResponseBody
    public List<TagRating> getTagsAndScores() {
        String url = "http://other-microservice-url/path/to/tdf-idf/service";


        ResponseEntity<TagRating[]> response = restTemplate.getForEntity(url, TagRating[].class);


        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to get the data from the other service.");
        }


        return Arrays.asList(response.getBody());
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }
    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<?> handleInvalidFileException(InvalidFileException exc) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(exc.getMessage());
    }


    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {

        Resource file = storageService.loadAsResource(filename);

        if (file == null)
            return ResponseEntity.notFound().build();

        ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                .filename(file.getFilename(), StandardCharsets.UTF_8) // here is the change
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .body(file);
    }
}