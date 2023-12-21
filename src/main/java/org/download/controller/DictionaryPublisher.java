package org.download.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Component
public class DictionaryPublisher {
    @Value("${spring.rabbitmq.host}")
    private String rabbitmqHost;

    @Value("${spring.rabbitmq.username}")
    private String rabbitmqUsername;

    @Value("${spring.rabbitmq.password}")
    private String rabbitmqPassword;

    @Value("${spring.rabbitmq.port}")
    private int rabbitmqPort;

    private final String QUEUE_NAME = "tdf_result";

    public void publishDictionary(Map<String, Double> dictionary) {
        Connection connection = null;
        Channel channel = null;

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(rabbitmqHost);
            factory.setUsername(rabbitmqUsername);
            factory.setPassword(rabbitmqPassword);
            factory.setPort(rabbitmqPort);

            connection = factory.newConnection();
            channel = connection.createChannel();

            // Определяем долговечность и тип очереди
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);

            ObjectMapper objectMapper = new ObjectMapper();

            String jsonString;
            try {
            // Преобразуем словарь в JSON строку
            jsonString = objectMapper.writeValueAsString(dictionary);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            // Обработка ошибки, если возникла проблема с преобразованием в JSON
            return;
        }
            try {
                // Отправляем JSON сообщение
                channel.basicPublish("", QUEUE_NAME, null, jsonString.getBytes(StandardCharsets.UTF_8));
                System.out.println(" [x] Sent '" + jsonString + "'");
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        } finally {
            try {
                if (channel != null && channel.isOpen()) {
                    channel.close();
                }
                if (connection != null && connection.isOpen()) {
                    connection.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
