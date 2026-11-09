package org.apache.fineract.retention;

import org.apache.fineract.export.TenantContext;
import org.apache.fineract.organisation.tenant.TenantServerConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VariableRetentionJobTest {

    private final TenantContext tenantContext = mock(TenantContext.class);
    private final VariableTableOps table = mock(VariableTableOps.class);
    private final VariableArchive archive = mock(VariableArchive.class);
    private final VariableRetentionProperties properties = new VariableRetentionProperties();
    private VariableRetentionJob job;

    @BeforeEach
    void setUp() {
        // 2027-11-02 03:00 UTC: with 12 months kept, 2026-10 and older are due
        job = new VariableRetentionJob(tenantContext, table, archive, properties,
                Clock.fixed(Instant.parse("2027-11-02T03:00:00Z"), ZoneOffset.UTC));
        TenantServerConnection kenya = new TenantServerConnection();
        kenya.setSchemaName("kenya");
        when(tenantContext.allTenants()).thenReturn(Collections.singletonList(kenya));
        doAnswer(i -> {
            ((Runnable) i.getArgument(1)).run();
            return null;
        }).when(tenantContext).runAs(any(TenantServerConnection.class), any(Runnable.class));
        when(table.tryLock(anyString())).thenReturn(true);
        when(table.partitionNames()).thenReturn(Arrays.asList("p2026_09", "p2026_10", "p2026_11", "p2027_12", "pmax"));
        when(table.rowCount("p2026_09")).thenReturn(0L);
        when(table.rowCount("p2026_10")).thenReturn(5L);
        when(archive.keyFor(anyString(), anyString())).thenReturn("key");
    }

    @Test
    @DisplayName("Adds the months ahead, archives before dropping, drops an empty month without archiving")
    void addsArchivesAndDrops() {
        when(archive.archive("kenya", "p2026_10")).thenReturn(5L);

        job.run();

        InOrder order = inOrder(table, archive);
        order.verify(table).addMonth(YearMonth.of(2028, 1));
        order.verify(table).addMonth(YearMonth.of(2028, 2));
        order.verify(table).dropPartition("p2026_09");
        order.verify(archive).archive("kenya", "p2026_10");
        order.verify(table).dropPartition("p2026_10");
        order.verify(table).unlock("kenya");
        verify(archive, never()).archive("kenya", "p2026_09");
        verify(table, never()).dropPartition("p2026_11");
    }

    @Test
    @DisplayName("Keeps the partition and stops for the tenant when the archive fails")
    void keepsPartitionWhenArchiveFails() {
        when(table.partitionNames()).thenReturn(Arrays.asList("p2026_09", "p2026_10", "p2027_12", "pmax"));
        when(table.rowCount("p2026_09")).thenReturn(3L);
        when(archive.archive("kenya", "p2026_09")).thenThrow(new IllegalStateException("S3 down"));

        job.run();

        verify(table, never()).dropPartition(anyString());
        verify(table).unlock("kenya");
    }

    @Test
    @DisplayName("Skips a tenant whose table is not partitioned, or that another pod is handling")
    void skipsUnpartitionedAndLockedTenants() {
        when(table.partitionNames()).thenReturn(Collections.emptyList());
        job.run();
        verify(table, never()).addMonth(any());
        verify(table).unlock("kenya");

        when(table.tryLock("kenya")).thenReturn(false);
        job.run();
        verify(table).unlock("kenya"); // still only the one unlock from the first run
    }

    @Test
    @DisplayName("A dry run changes nothing")
    void dryRun() {
        properties.setDryRun(true);
        job.run();
        verify(table, never()).addMonth(any());
        verify(archive, never()).archive(anyString(), anyString());
        verify(table, never()).dropPartition(anyString());
    }
}
