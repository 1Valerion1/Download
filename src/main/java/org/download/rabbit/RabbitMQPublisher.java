package org.download.rabbit;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.TimeoutException;

@Component
public class RabbitMQPublisher {
    @Autowired
    private ConnectionFactory connectionFactory;

    @Value("${spring.rabbitmq.host}")
    private String rabbitmqHost;

    private final String QUEUE_NAME = "file_queue";

    public void sendFileToQueue(Integer id ,byte[] fileData, String fileName) {
        try (Connection connection = connectionFactory.createConnection();
             Channel channel = connection.createChannel(false)) {
            // Кодируем содержимое файла
            String extractedText = extractTextFromDocx(fileData);
            byte[] extractedTextBytes = extractedText.getBytes("UTF-8");
            byte[]  extractedTextBytesBase64 = Base64.getEncoder().encode(extractedTextBytes);

            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                    .contentEncoding("UTF-8")
                    //.headers(Collections.singletonMap("file_name", fileName))
                    .headers(Collections.singletonMap("id", id))
                    .build();

            // Определяем долговечность и тип очереди
            channel.queueDeclare(QUEUE_NAME, true, false, false, null); // durable, not exclusive, not auto-delete
            channel.basicPublish("", QUEUE_NAME, properties, extractedTextBytesBase64); //encodedFileData
            System.out.println(" [x] Sent file to queue id:" +  id);

        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    private String extractTextFromDocx(byte[] fileData) throws IOException {

        XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(fileData));
        XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
        String text = extractor.getText();
        extractor.close();
        doc.close();
        return text;
    }
}
