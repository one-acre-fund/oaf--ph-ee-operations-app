package org.apache.fineract.file;

import com.azure.storage.blob.BlobClientBuilder;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@Qualifier("azureStorage")
@ConditionalOnProperty(
        value="cloud.azure.enabled",
        havingValue = "true")
public class AzureFileTransferImpl implements FileTransferService {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    BlobClientBuilder client;

    @Override
    public String uploadFile(File file, String bucketName) {

        String fileName = System.currentTimeMillis() + "_" + file.getName();
        try (InputStream inputStream = FileUtils.openInputStream(file)) {
            client.containerName(bucketName).blobName(fileName).buildClient().upload(inputStream, file.length());
        } catch (IOException e) {
            throw new UncheckedIOException("Error uploading file to Azure", e);
        }
        file.delete();
        return fileName;
    }

    @Override
    public byte[] downloadFile(String fileName, String bucketName) {
        Path temp = null;
        try {
            temp = Files.createTempFile("azure_download_", ".tmp");
            client.containerName(bucketName).blobName(fileName).buildClient().downloadToFile(temp.toString(), true);
            return Files.readAllBytes(temp);
        } catch (IOException e) {
            throw new UncheckedIOException("Error downloading file from Azure", e);
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException e) {
                    logger.warn("Could not delete temp file", e);
                }
            }
        }
    }

    @Override
    public void deleteFile(String fileName, String bucketName) {
        client.containerName(bucketName).blobName(fileName).buildClient().delete();
    }
}
