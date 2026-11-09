package org.apache.fineract.retention;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.apache.fineract.core.service.DataSourcePerTenantService;
import org.apache.fineract.file.FileTransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VariableArchiveTest {

    private static final String KEY = "variables-archive/kenya/variables-p2025_11.csv.gz";

    private final Connection connection = mock(Connection.class);
    private final Statement statement = mock(Statement.class);
    private final ResultSet rs = mock(ResultSet.class);
    private final FileTransferService files = mock(FileTransferService.class);
    private final AmazonS3 s3 = mock(AmazonS3.class);
    private final AtomicReference<File> uploaded = new AtomicReference<>();
    private final AtomicReference<String> uploadedCsv = new AtomicReference<>();
    private VariableArchive archive;

    @BeforeEach
    void setUp() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        DataSourcePerTenantService dataSources = mock(DataSourcePerTenantService.class);
        when(dataSources.retrieveDataSource()).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(anyInt(), anyInt())).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(rs);
        doAnswer(i -> {
            File file = i.getArgument(1);
            uploaded.set(file);
            uploadedCsv.set(gunzip(file));
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.length());
            when(s3.getObjectMetadata("bucket", KEY)).thenReturn(metadata);
            return null;
        }).when(files).uploadFile(eq(KEY), any(File.class), eq("bucket"));
        archive = new VariableArchive(dataSources, files, s3, "bucket", new VariableRetentionProperties());
    }

    @Test
    @DisplayName("Streams the partition into a gzip CSV, uploads it, checks the size and removes the temp file")
    void archives() throws SQLException {
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getString(1)).thenReturn("1", "2");
        when(rs.getString(2)).thenReturn("10", "20");
        when(rs.getString(3)).thenReturn("100", (String) null);
        when(rs.getString(4)).thenReturn("0", "1");
        when(rs.getString(5)).thenReturn("1762000000000", "1762000000001");
        when(rs.getString(6)).thenReturn("amount", "errorDescription");
        when(rs.getString(7)).thenReturn("20", "say \"no\", twice");

        assertEquals(2, archive.archive("kenya", "p2025_11"));

        verify(statement).setFetchSize(Integer.MIN_VALUE);
        verify(statement).executeQuery("SELECT `ID`, `WORKFLOW_INSTANCE_KEY`, `WORKFLOW_KEY`, `ZEEBE_GENERATION`,"
                + " `TIMESTAMP`, `NAME`, `VALUE` FROM variables PARTITION (p2025_11)");
        assertEquals("ID,WORKFLOW_INSTANCE_KEY,WORKFLOW_KEY,ZEEBE_GENERATION,TIMESTAMP,NAME,VALUE\r\n"
                + "1,10,100,0,1762000000000,amount,20\r\n"
                + "2,20,,1,1762000000001,errorDescription,\"say \"\"no\"\", twice\"\r\n", uploadedCsv.get());
        assertFalse(uploaded.get().exists());
    }

    @Test
    @DisplayName("Fails when the stored object is not the size of the local file")
    void sizeMismatch() throws SQLException {
        when(rs.next()).thenReturn(false);
        doAnswer(i -> {
            uploaded.set(i.getArgument(1));
            when(s3.getObjectMetadata("bucket", KEY)).thenReturn(new ObjectMetadata());
            return null;
        }).when(files).uploadFile(eq(KEY), any(File.class), eq("bucket"));

        assertThrows(IllegalStateException.class, () -> archive.archive("kenya", "p2025_11"));
        assertFalse(uploaded.get().exists());
    }

    @Test
    @DisplayName("Fails without uploading when the database read fails")
    void databaseFailure() throws SQLException {
        when(statement.executeQuery(anyString())).thenThrow(new SQLException("gone"));

        assertThrows(IllegalStateException.class, () -> archive.archive("kenya", "p2025_11"));
        verify(files, never()).uploadFile(anyString(), any(File.class), anyString());
    }

    @Test
    @DisplayName("Refuses anything that is not a partition name")
    void rejectsOtherNames() throws SQLException {
        assertThrows(IllegalArgumentException.class, () -> archive.archive("kenya", "p1) UNION SELECT 1 --"));
        verify(statement, never()).executeQuery(anyString());
    }

    private static String gunzip(File file) throws IOException {
        try (InputStream in = new GZIPInputStream(Files.newInputStream(file.toPath()))) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            for (int n; (n = in.read(buffer)) > 0; ) {
                out.write(buffer, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
