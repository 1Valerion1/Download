package org.download;

import org.download.controller.FileUploadController;
import org.download.exception.InvalidFileException;
import org.download.exception.StorageException;
import org.download.services.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileUploadController.class)
@ExtendWith(MockitoExtension.class)
public class FileUploadControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StorageService storageService;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    public void shouldUploadFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.docx",
                                        MediaType.MULTIPART_FORM_DATA_VALUE, "test data".getBytes());

        Mockito.when(storageService.handleFileUpload(any()))
                .thenReturn(Map.of("status", "success"));

        this.mockMvc.perform(multipart("/upload").file(file))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldNotUploadInvalidFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt",
                                MediaType.MULTIPART_FORM_DATA_VALUE, "test data".getBytes());

        doThrow(new InvalidFileException("Only .docx files are allowed!"))
                .when(storageService).
                handleFileUpload(any());

        this.mockMvc.perform(multipart("/upload").file(file))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    public void shouldNotUploadFileDueToStorageException() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.docx",
                MediaType.MULTIPART_FORM_DATA_VALUE, "test data".getBytes());

        doThrow(new StorageException("Could not initialize storage"))
                .when(storageService).
                store(any());

        this.mockMvc.perform(multipart("/upload").file(file))
                .andExpect(status().isBadRequest());
    }
}