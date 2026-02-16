package org.apache.fineract.organisation.role;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Immutable command for updating permissions (initially maker-checker).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PermissionsCommand {
    private Map<String, Boolean> permissions;

}
