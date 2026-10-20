package org.apache.fineract.organisation.parent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import javax.persistence.Column;
import javax.persistence.Version;
import org.apache.fineract.operations.Batch;
import org.apache.fineract.operations.TransactionRequest;
import org.apache.fineract.operations.Transfer;
import org.junit.jupiter.api.Test;

class VersionedPersistableCustomTest {

    @Test
    void rowsSharedWithTheImporterCarryTheOptimisticLock() {
        assertTrue(VersionedPersistableCustom.class.isAssignableFrom(Transfer.class));
        assertTrue(VersionedPersistableCustom.class.isAssignableFrom(TransactionRequest.class));
        assertTrue(VersionedPersistableCustom.class.isAssignableFrom(Batch.class));
    }

    @Test
    void versionMapsToTheColumnTheImporterMigrationAdds() throws Exception {
        Field version = VersionedPersistableCustom.class.getDeclaredField("version");
        assertTrue(version.isAnnotationPresent(Version.class));
        assertEquals("VERSION", version.getAnnotation(Column.class).name());
    }

    @Test
    void newRowsStartWithoutAVersionUntilHibernateAssignsOne() {
        Transfer transfer = new Transfer();
        assertNull(transfer.getVersion());
        transfer.setVersion(3L);
        assertEquals(Long.valueOf(3L), transfer.getVersion());
    }
}
