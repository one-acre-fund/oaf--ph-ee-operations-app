package org.apache.fineract.export;

import org.apache.fineract.file.FileTransferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Paths;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ExportStorageBucketTest {

    private final FileTransferService files = mock(FileTransferService.class);

    @Test
    @DisplayName("Exports use export.bucket-name when it is set")
    void usesExportBucket() {
        upload(new MockEnvironment()
                .withProperty("application.bucket-name", "shared")
                .withProperty("export.bucket-name", "exports-only"));

        verify(files).uploadFile("k", Paths.get("/tmp/x.csv").toFile(), "exports-only");
    }

    @Test
    @DisplayName("Exports fall back to application.bucket-name when export.bucket-name is not set")
    void fallsBackToApplicationBucket() {
        upload(new MockEnvironment().withProperty("application.bucket-name", "shared"));

        verify(files).uploadFile("k", Paths.get("/tmp/x.csv").toFile(), "shared");
    }

    private void upload(MockEnvironment environment) {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.setEnvironment(environment);
            context.registerBean("awsStorage", FileTransferService.class, () -> files);
            context.registerBean(ExportProperties.class);
            context.registerBean(PropertySourcesPlaceholderConfigurer.class);
            context.registerBean(ExportStorage.class);
            context.refresh();
            context.getBean(ExportStorage.class).upload("k", Paths.get("/tmp/x.csv"));
        }
    }
}
