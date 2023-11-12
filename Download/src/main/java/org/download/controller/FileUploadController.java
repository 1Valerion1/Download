package org.download.controller;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.download.model.TagRating;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.download.exception.StorageFileNotFoundException;
import org.download.services.StorageService;

@Controller
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

        model.addAttribute("files", storageService.loadAll().map(
                        path -> MvcUriComponentsBuilder.fromMethodName(FileUploadController.class,
                                "serveFile", path.getFileName().toString()).build().toUri().toString())
                .collect(Collectors.toList()));

        return "uploadForm";
    }

    // Загружаем док и отправляем на другйо микросервис дял обработки и анализа
    @PostMapping("/")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {

        storageService.store(file);
        redirectAttributes.addFlashAttribute("message",
                "You successfully uploaded " + file.getOriginalFilename() + "!");

        // Здесь мы отправим файл в другой микросервис
        try {
            //Создайте HttpEntity с файлом и заголовками
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body
                    = new LinkedMultiValueMap<>();

            FileSystemResource value
                    = new FileSystemResource(new File(storageService.load(file.getOriginalFilename()).toString()));

            body.add("file", value);

            HttpEntity<MultiValueMap<String, Object>> requestEntity
                    = new HttpEntity<>(body, headers);

            // Execute the HTTP Request
            ResponseEntity<String> response =
                    restTemplate
                    .exchange("http://other-microservice-url/path/to/tdf-idf/service", // URL микросервиса, который собирается обрабатывать файл
                            HttpMethod.POST, requestEntity, String.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send the file to the other service.", e);
        }

        return "redirect:/";
    }

    // Возвращаем список  тегов и их оценку от микросервиса по его обработке и анализа
    @GetMapping("/getTagsAndScores")
    @ResponseBody
    public List<TagRating> getTagsAndScores() {
        // затем в коде вызовем другой микросервис, чтобы получить данные.
        String url = "http://other-microservice-url/path/to/tdf-idf/service"; // url микросервиса, откуда будем получать данные

        // Данный REST-запрос будет получать массив объектов TagRating
        ResponseEntity<TagRating[]> response = restTemplate.getForEntity(url, TagRating[].class);

        // Если HTTP-ответ не успешен, бросим исключение
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to get the data from the other service.");
        }

        // Вернем данные ввиде списка если все ок.
        return Arrays.asList(response.getBody());
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }


//    @GetMapping("/files/{filename:.+}")
//    @ResponseBody
//    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
//
//        Resource file = storageService.loadAsResource(filename);
//
//        if (file == null)
//            return ResponseEntity.notFound().build();
//
//        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
//                "attachment; filename=\"" + file.getFilename() + "\"").body(file);
//    }
}