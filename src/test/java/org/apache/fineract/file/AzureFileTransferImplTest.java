package org.apache.fineract.file;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AzureFileTransferImplTest {

    private static final String CONTAINER = "test-container";

    private Path tempDir;

    private BlobClientBuilder builder;
    private BlobClient blobClient;
    private AzureFileTransferImpl service;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("file-transfer-test");
        builder = mock(BlobClientBuilder.class);
        blobClient = mock(BlobClient.class);
        when(builder.containerName(anyString())).thenReturn(builder);
        when(builder.blobName(anyString())).thenReturn(builder);
        when(builder.buildClient()).thenReturn(blobClient);
        service = new AzureFileTransferImpl();
        ReflectionTestUtils.setField(service, "client", builder);
    }

    @DisplayName("uploadFile uploads a timestamp-prefixed blob, returns its name and deletes the local file")
    @AfterEach
    void tearDown() throws IOException {
        FileUtils.deleteDirectory(tempDir.toFile());
    }

    @Test
    void testUploadFile() throws Exception {
        byte[] content = "a,b".getBytes(StandardCharsets.UTF_8);
        File file = Files.write(tempDir.resolve("result.csv"), content).toFile();

        String blobName = service.uploadFile(file, CONTAINER);

        assertTrue(blobName.matches("\\d+_result\\.csv"), "Blob name should be <timestamp>_<name>: " + blobName);
        verify(builder).containerName(CONTAINER);
        verify(builder).blobName(blobName);
        verify(blobClient).upload(any(InputStream.class), eq((long) content.length));
        assertFalse(file.exists(), "Local file should be deleted after upload");
    }

    @DisplayName("uploadFile wraps an unreadable file in UncheckedIOException without calling Azure")
    @Test
    void testUploadFileMissing() {
        File missing = tempDir.resolve("missing.csv").toFile();

        UncheckedIOException ex = assertThrows(UncheckedIOException.class,
                () -> service.uploadFile(missing, CONTAINER));
        assertEquals("Error uploading file to Azure", ex.getMessage());
        verify(blobClient, never()).upload(any(InputStream.class), anyLong());
    }

    @DisplayName("downloadFile returns the blob's bytes and removes the temp file")
    @Test
    void testDownloadFile() {
        byte[] content = "file-content".getBytes(StandardCharsets.UTF_8);
        AtomicReference<Path> tempFile = new AtomicReference<>();
        when(blobClient.downloadToFile(anyString(), eq(true))).thenAnswer(inv -> {
            Path path = Paths.get(inv.<String>getArgument(0));
            tempFile.set(path);
            Files.write(path, content);
            return null;
        });

        byte[] result = service.downloadFile("key.csv", CONTAINER);

        assertArrayEquals(content, result);
        verify(builder).containerName(CONTAINER);
        verify(builder).blobName("key.csv");
        assertFalse(Files.exists(tempFile.get()), "Temp file should be deleted after download");
    }

    @DisplayName("downloadFile wraps read failures in UncheckedIOException and still removes the temp file")
    @Test
    void testDownloadFileReadFailure() {
        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        // Deleting the temp file makes the subsequent read fail with an IOException
        when(blobClient.downloadToFile(pathCaptor.capture(), eq(true))).thenAnswer(inv -> {
            Files.delete(Paths.get(inv.<String>getArgument(0)));
            return null;
        });

        UncheckedIOException ex = assertThrows(UncheckedIOException.class,
                () -> service.downloadFile("key.csv", CONTAINER));
        assertEquals("Error downloading file from Azure", ex.getMessage());
        assertFalse(Files.exists(Paths.get(pathCaptor.getValue())));
    }

    @DisplayName("downloadFile removes the temp file when the Azure call fails")
    @Test
    void testDownloadFileAzureFailure() {
        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        when(blobClient.downloadToFile(pathCaptor.capture(), eq(true)))
                .thenThrow(new RuntimeException("blob not found"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.downloadFile("key.csv", CONTAINER));
        assertEquals("blob not found", ex.getMessage());
        assertFalse(Files.exists(Paths.get(pathCaptor.getValue())), "Temp file should be deleted on failure");
    }

    @DisplayName("deleteFile deletes the blob")
    @Test
    void testDeleteFile() {
        service.deleteFile("key.csv", CONTAINER);

        verify(builder).containerName(CONTAINER);
        verify(builder).blobName("key.csv");
        verify(blobClient).delete();
    }
}
