
package org.download.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.download.exception.InvalidFileException;
import org.download.rabbit.RabbitMQConsumer;
import org.download.rabbit.RabbitMQPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import org.download.exception.StorageFileNotFoundException;
import org.download.services.StorageService;


@RestController
@CrossOrigin("*")
public class FileUploadController {
    private static Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private RabbitMQPublisher rabbitMQPublisher;

    @Autowired
    private RabbitMQConsumer rabbitMQConsumer;

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

    @Async
    @PostMapping("/analyse")
    public CompletableFuture<ResponseEntity<String>> handleFileUpload(@RequestParam("file") MultipartFile file) {
        logger.info("Handling handleFileUpload request");

        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> response = storageService.handleFileUpload(file);
            HttpStatus status = HttpStatus.OK;

            if (response.get("status") != null && response.get("status").equals("success")) {
                try {
                    Integer id = getNextId();
                    rabbitMQPublisher.sendFileToQueue(id, file.getBytes(), file.getOriginalFilename());
                    rabbitMQConsumer.listenForResultsAndSendToFrontend();

                    return new ResponseEntity<>(id.toString(), status);
                } catch (IOException e) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to send file to processing queue");
                }
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, (String) response.get("message"));
            }
        });
    }


    @GetMapping("/result/{id}")
    @ResponseBody
    @Async
    public CompletableFuture<ResponseEntity<Map<String, Double>>> getResult(@PathVariable String id) {
        if (id == null || id.isEmpty()) {
            // Если id пустой, вернуть соответствующую ошибку
            Map<String, Double> errorResult = new HashMap<>();
            errorResult.put("error", 400.0);
            return CompletableFuture.completedFuture(new ResponseEntity<>(errorResult, HttpStatus.BAD_REQUEST));
        }
        try {
            Integer intId = Integer.parseInt(id);
            Map<String, Double> result = storageService.getResult(intId);

            if (result == null) {
                // Если результат еще не готов, возвращаем статус 202
                result = new HashMap<>();
                result.put("status", 202.0);
                return CompletableFuture.completedFuture(new ResponseEntity<>(result, HttpStatus.ACCEPTED));
            } else {
                // Проверяем, что результат уже существует и содержит данные
                if (!result.isEmpty()) {
                    return CompletableFuture.completedFuture(new ResponseEntity<>(result, HttpStatus.OK));
                } else {
                    // Если результат пуст, возвращаем ошибку
                    Map<String, Double> emptyResultError = new HashMap<>();
                    emptyResultError.put("error", 404.0);
                    return CompletableFuture.completedFuture(new ResponseEntity<>(emptyResultError, HttpStatus.NOT_FOUND));
                }
            }
        } catch (NumberFormatException e) {
            // Если id не удалось преобразовать в Integer, вернуть соответствующую ошибку
            Map<String, Double> errorResult = new HashMap<>();
            errorResult.put("error", 400.0);
            return CompletableFuture.completedFuture(new ResponseEntity<>(errorResult, HttpStatus.BAD_REQUEST));
        }
    }

    private Integer currentId = 0;

    private synchronized Integer getNextId() {
        currentId++;
        return currentId;
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