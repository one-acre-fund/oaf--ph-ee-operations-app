package org.apache.fineract.audit.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.fineract.organisation.user.UserDto;

import java.util.List;

@Data
@AllArgsConstructor
public class AuditTemplateResponse {
    private List<UserDto> appUsers;
    private List<String> actionNames;
    private List<String> entityNames;
}
