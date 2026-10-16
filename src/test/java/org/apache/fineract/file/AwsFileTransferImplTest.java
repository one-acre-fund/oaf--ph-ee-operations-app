package org.apache.fineract.file;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AwsFileTransferImplTest {

    private static final String BUCKET = "test-bucket";

    private Path tempDir;

    private AmazonS3 s3Client;
    private AwsFileTransferImpl service;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("file-transfer-test");
        s3Client = mock(AmazonS3.class);
        service = new AwsFileTransferImpl();
        ReflectionTestUtils.setField(service, "s3Client", s3Client);
    }

    @AfterEach
    void tearDown() throws IOException {
        FileUtils.deleteDirectory(tempDir.toFile());
    }

    @DisplayName("uploadFile puts a timestamp-prefixed object, returns its URL and deletes the local file")
    @Test
    void testUploadFile() throws Exception {
        File file = Files.write(tempDir.resolve("result.csv"), "a,b".getBytes()).toFile();
        when(s3Client.getUrl(eq(BUCKET), anyString()))
                .thenAnswer(inv -> new URL("https://s3.example.com/" + BUCKET + "/" + inv.getArgument(1)));

        String url = service.uploadFile(file, BUCKET);

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture());
        PutObjectRequest request = captor.getValue();
        assertEquals(BUCKET, request.getBucketName());
        assertTrue(request.getKey().matches("\\d+_result\\.csv"), "Key should be <timestamp>_<name>: " + request.getKey());
        assertEquals(file, request.getFile());
        assertEquals("https://s3.example.com/" + BUCKET + "/" + request.getKey(), url);
        assertFalse(file.exists(), "Local file should be deleted after upload");
    }

    @DisplayName("downloadFile returns the object's bytes")
    @Test
    void testDownloadFile() {
        byte[] content = "file-content".getBytes(StandardCharsets.UTF_8);
        S3Object s3Object = mock(S3Object.class);
        when(s3Object.getObjectContent()).thenReturn(s3Stream(new ByteArrayInputStream(content)));
        when(s3Client.getObject(BUCKET, "key.csv")).thenReturn(s3Object);

        assertArrayEquals(content, service.downloadFile("key.csv", BUCKET));
    }

    @DisplayName("downloadFile wraps read failures in UncheckedIOException")
    @Test
    void testDownloadFileReadFailure() {
        InputStream failing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("connection reset");
            }
        };
        S3Object s3Object = mock(S3Object.class);
        when(s3Object.getObjectContent()).thenReturn(s3Stream(failing));
        when(s3Client.getObject(BUCKET, "key.csv")).thenReturn(s3Object);

        UncheckedIOException ex = assertThrows(UncheckedIOException.class,
                () -> service.downloadFile("key.csv", BUCKET));
        assertEquals("Error downloading file from S3", ex.getMessage());
        assertEquals("connection reset", ex.getCause().getMessage());
    }

    @DisplayName("deleteFile deletes the object from the bucket")
    @Test
    void testDeleteFile() {
        service.deleteFile("key.csv", BUCKET);

        verify(s3Client).deleteObject(BUCKET, "key.csv");
    }

    private S3ObjectInputStream s3Stream(InputStream in) {
        // S3ObjectInputStream aborts the HTTP request on close, so it needs a non-null request
        return new S3ObjectInputStream(in, mock(HttpRequestBase.class));
    }
}
