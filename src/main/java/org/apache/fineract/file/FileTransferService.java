package org.apache.fineract.file;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.OutputStream;

@Service
public interface FileTransferService {

    String uploadFile(File file, String bucketName);

    byte[] downloadFile(String fileName, String bucketName);

    void deleteFile(String fileName, String bucketName);

    /**
     * Uploads a file under the given key, streaming it from disk. The local file is left for the caller to delete.
     */
    void uploadFile(String key, File file, String bucketName);

    /**
     * Streams the object to the given output, without loading it in memory.
     */
    void downloadFile(String key, String bucketName, OutputStream out);

}
