package org.download.services;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

public interface StorageService {

    void init();

    void save(Integer id, Map<String, Double> messageMap);

    public Map<String, Double> getResult(Integer id);


    void store(MultipartFile file);

    Stream<Path> loadAll();

    Path load(String filename);

    Resource loadAsResource(String filename);

    void deleteAll();

    String sendFileToOtherService(MultipartFile file);

    Map<String, Object> handleFileUpload(MultipartFile file);
}