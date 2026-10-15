package org.apache.fineract.file;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

@Service
@Qualifier("awsStorage")
public class AwsFileTransferImpl implements FileTransferService {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private AmazonS3 s3Client;

    @Override
    public String uploadFile(File file, String bucketName) {

        String fileName = System.currentTimeMillis() + "_" + file.getName();
        s3Client.putObject(new PutObjectRequest(bucketName, fileName, file));
        String url = s3Client.getUrl(bucketName, fileName).toString();
        file.delete();
        return url;
    }

    @Override
    public byte[] downloadFile(String fileName, String bucketName) {
        try (S3Object s3Object = s3Client.getObject(bucketName, fileName);
             S3ObjectInputStream inputStream = s3Object.getObjectContent()) {
            return IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException("Error downloading file from S3", e);
        }
    }

    @Override
    public void deleteFile(String fileName, String bucketName) {
        s3Client.deleteObject(bucketName, fileName);
    }

    private File convertMultiPartFileToFile(MultipartFile file) {
        File convertedFile = new File(file.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(convertedFile)) {
            fos.write(file.getBytes());
        } catch (IOException e) {
            logger.error("Error converting multipartFile to file", e);
        }
        return convertedFile;
    }
}
