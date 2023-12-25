package org.download;

import com.rabbitmq.client.AMQP;
import org.download.controller.FileUploadController;
import org.download.rabbit.RabbitMQConsumer;
import org.download.rabbit.RabbitMQPublisher;
import org.download.services.StorageService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.data.repository.util.ClassUtils.hasProperty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;


@WebMvcTest(FileUploadController.class)
@ExtendWith(MockitoExtension.class)
public class FileUploadControllerTest {
    @InjectMocks
    private FileUploadController fileUploadController;

    @Mock
    private StorageService storageService;

    @Mock
    private RabbitMQPublisher rabbitMQPublisher;

    @Mock
    private RabbitMQConsumer rabbitMQConsumer;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private MockMvc mockMvc;

//    @Test
//    public void testFileUpload() throws Exception {
//        // Создаем фиктивный файл для загрузки
//        MockMultipartFile file = new MockMultipartFile(
//                "file",
//                "filename.docx",
//                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
//                "Пример текста в документе".getBytes()
//        );
//
//        // Выполняем запрос на загрузку файла
//        mockMvc.perform(multipart("/analyse").file(file))
//                .andExpect(status().isOk()); // Проверяем, что ответ имеет статус 200 OK
//    }
//
//
//    @Test
//    public void GetResulNotReady() throws Exception {
//        MvcResult mvcResult = mockMvc.perform(get("/result/1")
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(request().asyncStarted())
//                .andReturn();
//
//        mvcResult.getAsyncResult();
//
//        mockMvc.perform(asyncDispatch(mvcResult))
//                .andExpect(status().isAccepted())
//                .andExpect(jsonPath("$.status").value(202));
//    }
//
//    @Test
//    public void testSendFileToQueue() throws Exception {
//        byte[] fileData = "example file data".getBytes();
//        String fileName = "example.docx";
//        Integer id = 1;
//
//        //отправку файла в очередь
//        rabbitMQPublisher.sendFileToQueue(id, fileData, fileName);
//        // Проверяем, что метод sendFileToQueue был вызван один раз с указанными параметрами
//        verify(rabbitMQPublisher, times(1)).sendFileToQueue(id, fileData, fileName);
//    }

}