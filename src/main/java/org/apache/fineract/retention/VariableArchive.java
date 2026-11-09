package org.apache.fineract.retention;

import com.amazonaws.services.s3.AmazonS3;
import org.apache.fineract.core.service.DataSourcePerTenantService;
import org.apache.fineract.file.FileTransferService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.supercsv.io.CsvListWriter;
import org.supercsv.io.ICsvListWriter;
import org.supercsv.prefs.CsvPreference;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.zip.GZIPOutputStream;

/**
 * Copies one partition of the variables table to the export bucket as gzip CSV before it is dropped. The rows
 * are streamed from MySQL and written through a temporary file, so memory stays flat whatever the month holds.
 */
@Component
@ConditionalOnProperty(value = "variables.retention.enabled", havingValue = "true")
public class VariableArchive {

    static final String[] COLUMNS = {"ID", "WORKFLOW_INSTANCE_KEY", "WORKFLOW_KEY", "ZEEBE_GENERATION", "TIMESTAMP",
            "NAME", "VALUE"};

    private final DataSourcePerTenantService dataSources;
    private final FileTransferService fileTransferService;
    private final AmazonS3 s3;
    private final String bucketName;
    private final VariableRetentionProperties properties;

    public VariableArchive(DataSourcePerTenantService dataSources,
                           @Qualifier("awsStorage") FileTransferService fileTransferService, AmazonS3 s3,
                           @Value("${export.bucket-name}") String bucketName, VariableRetentionProperties properties) {
        this.dataSources = dataSources;
        this.fileTransferService = fileTransferService;
        this.s3 = s3;
        this.bucketName = bucketName;
        this.properties = properties;
    }

    public String keyFor(String tenant, String partition) {
        return properties.getStoragePrefix() + tenant + "/variables-" + partition + ".csv.gz";
    }

    /**
     * Exports, uploads and verifies the object (same size as the local file) before returning. Any failure
     * throws, and the caller must then keep the partition.
     *
     * @return rows written
     */
    public long archive(String tenant, String partition) {
        String key = keyFor(tenant, partition);
        Path file = null;
        try {
            file = Files.createTempFile("variables_" + partition + "_", ".csv.gz",
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            long rows = writeGzipCsv(partition, file);
            fileTransferService.uploadFile(key, file.toFile(), bucketName);
            long stored = s3.getObjectMetadata(bucketName, key).getContentLength();
            long local = Files.size(file);
            if (stored != local) {
                throw new IllegalStateException("Archive " + key + " holds " + stored + " bytes, expected " + local);
            }
            return rows;
        } catch (IOException e) {
            throw new UncheckedIOException("Archive " + key + " failed", e);
        } catch (SQLException e) {
            throw new IllegalStateException("Archive " + key + " failed", e);
        } finally {
            if (file != null) {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException ignored) {
                    // the temp file is in the pod's ephemeral storage; nothing more to do about it
                }
            }
        }
    }

    private long writeGzipCsv(String partition, Path file) throws IOException, SQLException {
        long rows = 0;
        try (Connection connection = dataSources.retrieveDataSource().getConnection();
             Statement statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
             ICsvListWriter csv = new CsvListWriter(new OutputStreamWriter(new GZIPOutputStream(
                     new BufferedOutputStream(Files.newOutputStream(file))), StandardCharsets.UTF_8),
                     CsvPreference.STANDARD_PREFERENCE)) {
            statement.setFetchSize(Integer.MIN_VALUE); // MySQL streams the result instead of buffering it
            csv.writeHeader(COLUMNS);
            String sql = "SELECT " + String.join(", ", quoted(COLUMNS))
                    + " FROM variables PARTITION (" + VariablePartitions.checked(partition) + ")";
            try (ResultSet rs = statement.executeQuery(sql)) { // NOSONAR partition name checked against pmax|pYYYY_MM
                String[] row = new String[COLUMNS.length];
                while (rs.next()) {
                    for (int i = 0; i < COLUMNS.length; i++) {
                        row[i] = rs.getString(i + 1);
                    }
                    csv.write((Object[]) row);
                    rows++;
                }
            }
        }
        return rows;
    }

    private static String[] quoted(String[] columns) {
        String[] out = new String[columns.length];
        for (int i = 0; i < columns.length; i++) {
            out[i] = "`" + columns[i] + "`";
        }
        return out;
    }
}
