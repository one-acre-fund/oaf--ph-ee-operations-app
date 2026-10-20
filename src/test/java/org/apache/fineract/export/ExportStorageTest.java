package org.apache.fineract.export;

import org.apache.fineract.file.FileTransferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ExportStorageTest {

    private final FileTransferService files = mock(FileTransferService.class);
    private final ExportStorage storage = new ExportStorage(files, "bucket", new ExportProperties());

    @Test
    @DisplayName("One object per run under exports/{tenant}/{jobId}-{attempt}.csv, uploaded, streamed and deleted via the file "
            + "service")
    void keysUploadDownloadAndDelete() {
        String key = storage.keyFor("oaf", "job-1", 2);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        storage.upload(key, Paths.get("/tmp/x.csv"));
        storage.copyTo(key, out);
        storage.delete(key);

        assertEquals("exports/oaf/job-1-2.csv", key);
        verify(files).uploadFile(key, Paths.get("/tmp/x.csv").toFile(), "bucket");
        verify(files).downloadFile(key, "bucket", out);
        verify(files).deleteFile(key, "bucket");
    }
}
