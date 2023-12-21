package org.download.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.rabbitmq.client.Channel;
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
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ConnectionFactory connectionFactory;

    private String resultQueueName = "tdf_result";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void listenForResultsAndSendToFrontend() {
        try (Connection connection = connectionFactory.createConnection();
             Channel channel = connection.createChannel(false)) {

            channel.queueDeclare(resultQueueName, true, false, false, null);

            com.rabbitmq.client.DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                byte[] messageBody = delivery.getBody();
                Map<String, Double> messageMap = convertMessageToMap(messageBody);
                sendToFrontend(messageMap);
            };

            channel.basicConsume(resultQueueName, true, deliverCallback, consumerTag -> {});
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    private Map<String, Double> convertMessageToMap(byte[] messageBody) {
        try {
            String jsonString = new String(messageBody, StandardCharsets.UTF_8);
            return objectMapper.readValue(jsonString, new TypeReference<Map<String, Double>>() {});
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }

    private void sendToFrontend(Map<String, Double> messageMap) {
        try {
            String url = "http://localhost:8089/result";
            restTemplate.postForObject(url, messageMap, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
