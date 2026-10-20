package org.apache.fineract.export;

import org.apache.fineract.file.FileTransferService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.nio.file.Path;

@Component
public class ExportStorage {

    private final FileTransferService fileTransferService;
    private final String bucketName;
    private final ExportProperties properties;

    public ExportStorage(@Qualifier("awsStorage") FileTransferService fileTransferService,
                         @Value("${application.bucket-name}") String bucketName, ExportProperties properties) {
        this.fileTransferService = fileTransferService;
        this.bucketName = bucketName;
        this.properties = properties;
    }

    public String keyFor(String tenant, String jobId) {
        return properties.getStoragePrefix() + tenant + "/" + jobId + ".csv";
    }

    public void upload(String key, Path file) {
        fileTransferService.uploadFile(key, file.toFile(), bucketName);
    }

    public void copyTo(String key, OutputStream out) {
        fileTransferService.downloadFile(key, bucketName, out);
    }

    public void delete(String key) {
        fileTransferService.deleteFile(key, bucketName);
    }
}
