package org.apache.fineract.api;

import com.azure.core.annotation.QueryParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.audit.data.AuditSearch;
import org.apache.fineract.audit.data.AuditTemplateResponse;
import org.apache.fineract.audit.service.AuditService;
import org.apache.fineract.audit.data.AuditSource;
import org.apache.fineract.audit.specs.AuditSpec;
import org.apache.fineract.utils.DateUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;


@RestController
@SecurityRequirement(name = "auth")
@RequestMapping("/audits")
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
                                                  @QueryParam("makerDateTimeFrom") @Parameter(description = "makerDateTimeFrom") final String makerDateTimeFrom,
                                                      @QueryParam("makerDateTimeTo") @Parameter(description = "makerDateTimeTo") final String makerDateTimeTo,
                                                  @QueryParam("processingResult") @Parameter(description = "processingResult") final String processingResult,
                                                  @RequestParam(value = "page", required = false) @Parameter(description = "page") final Integer page,
                                                  @RequestParam(value = "limit", required = false) @Parameter(description = "limit") final Integer limit,
                                                  @RequestParam(value = "orderBy", required = false) @Parameter(description = "orderBy") final String orderBy,
                                                  @RequestParam(value = "sortOrder", required = false) @Parameter(description = "sortOrder") final String sortOrder,
                                                  @QueryParam("dateFormat") @Parameter(description = "dateFormat") final String dateFormat) {
        AuditSpec auditSpec = new AuditSpec();
        LocalDateTime parsedMakerDateFrom = null;
        LocalDateTime parsedMakerDateTo = null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
        if(Objects.nonNull(makerDateTimeFrom)) {
            parsedMakerDateFrom = DateUtil.parseDateTime(makerDateTimeFrom, formatter);
        }
        if(Objects.nonNull(makerDateTimeTo)) {
            parsedMakerDateTo = DateUtil.parseDateTime(makerDateTimeTo, formatter);
        }

        AuditSearch search = new AuditSearch(actionName, entityName, resourceId, makerId, parsedMakerDateFrom, parsedMakerDateTo, processingResult);
        int defaultPage = (page != null) ? page : 0;
        int defaultLimit = (limit != null) ? limit : 10;
        String defaultOrderBy = (StringUtils.isNotEmpty(orderBy)) ? orderBy : "id";
        Sort.Direction defaultSortOrder = (StringUtils.isNotEmpty(sortOrder)) ? Sort.Direction.valueOf(sortOrder) : Sort.Direction.ASC;

        return this.auditService.getAudits(auditSpec.getFilter(search), PageRequest.of(defaultPage, defaultLimit, Sort.by(defaultSortOrder, defaultOrderBy)));
    }
    @GetMapping("/searchtemplate")
    public AuditTemplateResponse retrieveAuditTemplate() {
        return this.auditService.retrieveAuditTemplate();
    }

    @GetMapping("/{id}")
    public AuditSource getAudit(@PathVariable("id") Long id) {
        return this.auditService.findById(id);
    }
}
