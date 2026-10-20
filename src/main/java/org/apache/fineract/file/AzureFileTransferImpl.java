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
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

@Service
@Qualifier("azureStorage")
@ConditionalOnProperty(
        value="cloud.azure.enabled",
        havingValue = "true")
public class AzureFileTransferImpl implements FileTransferService {
    private static final FileAttribute<Set<PosixFilePermission>> OWNER_ONLY =
            PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"));

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
            temp = Files.createTempFile("azure_download_", ".tmp", OWNER_ONLY);
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

    @Override
    public void uploadFile(String key, File file, String bucketName) {
        client.containerName(bucketName).blobName(key).buildClient().uploadFromFile(file.getPath(), true);
    }

    @Override
    public void downloadFile(String key, String bucketName, OutputStream out) {
        client.containerName(bucketName).blobName(key).buildClient().download(out);
    }
}
