package org.download.config;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Configuration
public class RabbitMQConfiguration {
    private final String QUEUE_NAME = "file_queue";
    private String QUEUE_NAMERES = "tdf_result";
    @Value("${spring.rabbitmq.host}")
    private String rabbitmqHost;

    @Value("${spring.rabbitmq.username}")
    private String rabbitmqUsername;

    @Value("${spring.rabbitmq.password}")
    private String rabbitmqPassword;

    @Value("${spring.rabbitmq.port}")
    private int rabbitmqPort;

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(rabbitmqHost);
        connectionFactory.setUsername(rabbitmqUsername);
        connectionFactory.setPassword(rabbitmqPassword);
        connectionFactory.setPort(rabbitmqPort);
        return connectionFactory;
    }

    @Bean
    public Queue fileQueue() {
        return new Queue(QUEUE_NAME, true);
    }
    @Bean
    public Queue resultQueue() {
        return new Queue(QUEUE_NAMERES, true);
    }
    @Bean
    public CommandLineRunner declareQueue(ConnectionFactory connectionFactory) {
        return args -> {
            try (Connection connection = connectionFactory.createConnection();
                 Channel channel = connection.createChannel(false)) {
                channel.queueDeclare(QUEUE_NAME, true, false, false, null);
                System.out.println(" [*] Queue declared: " + QUEUE_NAME);
                channel.queueDeclare(QUEUE_NAMERES, true, false, false, null);
                System.out.println(" [*] Queue declared: " + QUEUE_NAMERES);
            } catch (IOException | TimeoutException e) {
                e.printStackTrace();
            }
        };
    }
    @Bean
    public RabbitTemplate rabbitTemplate() {
        return new RabbitTemplate(connectionFactory());
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}