package org.apache.fineract.organisation.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class UserWithRolesDto {
    private Long id;
    private String username;
    private List<String> roles;
}
