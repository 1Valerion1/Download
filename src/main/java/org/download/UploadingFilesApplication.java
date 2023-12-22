package org.download;

import org.download.config.RabbitMQConfiguration;
import org.download.config.StorageProperties;
import org.download.services.StorageService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableConfigurationProperties({StorageProperties.class})
@ComponentScan("org.download")
public class UploadingFilesApplication
{
    public static void main(String[] args) {
        SpringApplication.run(UploadingFilesApplication .class, args);


    }



    @Bean
    CommandLineRunner init(StorageService storageService) {
        return (args) -> {
            storageService.deleteAll();
            storageService.init();
        };
    }

}
