
package org.download.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.download.exception.InvalidFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.download.exception.StorageFileNotFoundException;
import org.download.services.StorageService;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@CrossOrigin("*")
public class FileUploadController {
    private static Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private RabbitMQPublisher rabbitMQPublisher;
    @Autowired
    private DictionaryPublisher dictionaryPublisher;

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

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> handleFileUpload(@RequestParam("file") MultipartFile file,
                                                                RedirectAttributes redirectAttributes) {
        logger.info("Handling handleFileUpload request");

        Map<String, Object> response = storageService.handleFileUpload(file);

        Object statusObject = response.get("status");
        HttpStatus status;

        if(statusObject != null && statusObject.equals("success")) {
            status = HttpStatus.OK;
            // Отправка содержимого файла в очередь для обработки
            try {
                rabbitMQPublisher.sendFileToQueue(file.getBytes(),file.getOriginalFilename());

                Map<String, Double> data = new HashMap<>();
                data.put("top", 0.5);
                data.put("chess", 0.3);
                dictionaryPublisher.publishDictionary(data);
                rabbitMQConsumer.listenForResultsAndSendToFrontend();

            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to send file to processing queue");
            }

        } else if (statusObject != null && statusObject.equals("error")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, (String) response.get("message"));
        } else {
            status = HttpStatus.BAD_REQUEST;
        }
        return new ResponseEntity<>(response, status);
    }


    @PostMapping("/send")
    public String sendFile(MultipartFile file) {
        try {
            logger.info("Handling sendFileToOtherService request" + file.getOriginalFilename());

            return storageService.sendFileToOtherService(file);

        } catch (HttpStatusCodeException e){
            logger.error("HttpStatusCodeException occurred: " + e.getMessage());
            if (e.getStatusCode().is5xxServerError()) {
                return "Сервис временно недоступен. Попробуйте позже.";
            } else {
                return "Произошла ошибка при отправке файла на другой сервис: " + e.getMessage();
            }
        } catch (RestClientException e) {
            logger.error("RestClientException occurred: " + e.getMessage());
            throw new RuntimeException("Не удалось отправить файл на другой сервис. Ошибка: " + e.getMessage());
        }
    }

    @PostMapping("/result")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getResult(@RequestParam Map<String, String> queryParams) {

        logger.info("Запрос на getResult получен"+queryParams);

        String url = "http://localhost:5173/Fresult";

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            builder.queryParam(entry.getKey(), entry.getValue());
        }


        ResponseEntity<Map<String, Object>> response =
                restTemplate.exchange(builder.toUriString(), HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {
                });

        return response;
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