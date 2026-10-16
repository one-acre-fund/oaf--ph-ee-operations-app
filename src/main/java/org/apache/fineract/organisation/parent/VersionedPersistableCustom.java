package org.apache.fineract.organisation.parent;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;
import lombok.Getter;
import lombok.Setter;

/**
 * Rows that ph-ee-importer-rdbms updates in place while this application may also write them
 * (transfers, transaction requests, batches). The optimistic lock column is shared with the
 * importer (FD-1931, tenant migration V53): a save that races an update of the same row fails
 * instead of overwriting it.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class VersionedPersistableCustom extends AbstractPersistableCustom<Long> {

    @Version
    @Column(name = "VERSION")
    private Long version;
}
