package org.apache.fineract.api;

import com.azure.core.annotation.QueryParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.bytebuddy.implementation.bind.annotation.Default;
import net.bytebuddy.implementation.bytecode.constant.DefaultValue;
import org.apache.fineract.audit.data.AuditSearch;
import org.apache.fineract.audit.service.AuditService;
import org.apache.fineract.audit.data.AuditSource;
import org.apache.fineract.audit.specs.AuditSpec;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;


@RestController
@SecurityRequirement(name = "BearerAuth")
@RequestMapping("/api/v1/audit")
@Tag(name = "Audit API")
public class AuditApiResource {
    private AuditService auditService;

    public AuditApiResource(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "List Audits", description = "Get a list of audits that match the criteria supplied and sorted by audit id in descending order, and are within the requestors' data scope. Also it supports pagination and sorting\n" + "\n" + "Example Request:\n" + "\n" + "audits\n" + "\n" + "audits?actionName=UPDATE&entityName=AppUser\n")
    public Page<AuditSource> retrieveAuditEntries(@QueryParam("actionName") @Parameter(description = "actionName") final String actionName,
                                                  @QueryParam("entityName") @Parameter(description = "entityName") final String entityName,
                                                  @QueryParam("resourceId") @Parameter(description = "resourceId") final Long resourceId,
                                                  @QueryParam("makerId") @Parameter(description = "makerId") final Long makerId,
                                                  @QueryParam("makerDateTimeFrom") @Parameter(description = "makerDateTimeFrom") final Date makerDateTimeFrom,
                                                  @QueryParam("makerDateTimeTo") @Parameter(description = "makerDateTimeTo") final Date makerDateTimeTo,
                                                  @QueryParam("processingResult") @Parameter(description = "processingResult") final String processingResult,
                                                  @RequestParam("page") @Parameter(description = "page") final Integer page,
                                                  @RequestParam("limit") @Parameter(description = "limit") final Integer limit,
                                                  @RequestParam("orderBy") @Parameter(description = "orderBy") final String orderBy,
                                                  @RequestParam("sortOrder") @Parameter(description = "sortOrder") final String sortOrder) {
        AuditSpec auditSpec = new AuditSpec();
        AuditSearch search = new AuditSearch(actionName, entityName, resourceId, makerId, makerDateTimeFrom, makerDateTimeTo, processingResult);
        return this.auditService.getAudits(auditSpec.getFilter(search), new PageRequest(page, limit, new Sort(Sort.Direction.valueOf(sortOrder), orderBy)));
    }
}
