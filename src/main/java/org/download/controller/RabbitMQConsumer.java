package org.download.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.download.services.FileSystemStorageService;
import org.download.services.StorageService;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Component
public class RabbitMQConsumer {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMQConsumer.class);

    @Autowired
    private ConnectionFactory connectionFactory;
    @Autowired
    private  StorageService storageService;
    private String resultQueueName = "tdf_result";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void listenForResultsAndSendToFrontend() {
        try (Connection connection = connectionFactory.createConnection();
             Channel channel = connection.createChannel(false)) {

            channel.queueDeclare(resultQueueName, true, false, false, null);

            com.rabbitmq.client.DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                // Извлекаем id из заголовков сообщения
                AMQP.BasicProperties properties = delivery.getProperties();
                Map<String, Object> headers = properties.getHeaders();

                Integer id = (Integer) headers.get("id");

                // Получаем тело сообщения
                byte[] messageBody = delivery.getBody();
                Map<String, Double> messageMap = convertMessageToMap(messageBody);

                logger.info("Данные переданы на сохранение id: " + id + ": " + messageMap);

                storageService.save(id,messageMap);
            };

            channel.basicConsume(resultQueueName, true, deliverCallback, consumerTag -> {});
        } catch (IOException | TimeoutException e) {
            logger.error("An error occurred while consuming messages from the result queue", e);
            e.printStackTrace();
        }
    }

    private Map<String, Double> convertMessageToMap(byte[] messageBody) {
        try {
            String jsonString = new String(messageBody, StandardCharsets.UTF_8);
            return objectMapper.readValue(jsonString, new TypeReference<Map<String, Double>>() {});
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("Error occurred while converting message to map", e);
            return Collections.emptyMap();
        }
    }


}
