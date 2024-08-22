
package org.apache.fineract.audit.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.organisation.parent.AbstractPersistableCustom;
import org.apache.fineract.organisation.user.AppUser;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity data class for audit entries.
 */
@Entity
@Table(name = "m_audit_source")
@Getter
@Setter
@NoArgsConstructor
public class AuditSource extends AbstractPersistableCustom<Long> {

    @Column(name = "action_name", nullable = true, length = 100)
    private String actionName;

    @Column(name = "entity_name", nullable = true, length = 100)
    private String entityName;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "data_as_json", length = 1000)
    private String dataAsJson;

    @ManyToOne
    @JoinColumn(name = "maker_id", nullable = false)
    private AppUser maker;

    @Column(name = "made_on_date", nullable = false)
    @JsonSerialize(using = com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime madeOnDate;

    @Column(name = "processing_result", nullable = false)
    private String processingResult;
}
