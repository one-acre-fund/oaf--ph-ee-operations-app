package org.apache.fineract.test;

import org.apache.fineract.organisation.permission.Permission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.persistence.Column;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the Permission entity changes related to MySQL 8.0 compatibility.
 * <p>
 * MySQL 8.0 made {@code GROUPING} a reserved keyword, so the column was renamed
 * from {@code grouping} to {@code module} to avoid conflicts.
 */
class PermissionEntityTest {

    @DisplayName("module field @Column name must map to 'module' for MySQL 8.0 compatibility")
    @Test
    void testModuleColumnName() throws NoSuchFieldException {
        Field moduleField = Permission.class.getDeclaredField("module");
        Column columnAnnotation = moduleField.getAnnotation(Column.class);

        assertNotNull(columnAnnotation, "@Column annotation must be present on 'module' field");
        assertEquals("module", columnAnnotation.name(),
                "Column name must be 'module' (renamed from 'grouping' which is a reserved keyword in MySQL 8.0)");
    }

    @DisplayName("module field @Column must be non-nullable with max length 45")
    @Test
    void testModuleColumnConstraints() throws NoSuchFieldException {
        Field moduleField = Permission.class.getDeclaredField("module");
        Column columnAnnotation = moduleField.getAnnotation(Column.class);

        assertNotNull(columnAnnotation);
        assertFalse(columnAnnotation.nullable(), "module column must be non-nullable");
        assertEquals(45, columnAnnotation.length(), "module column length must be 45");
    }

    @DisplayName("module getter and setter work correctly")
    @Test
    void testModuleGetterSetter() {
        Permission permission = new Permission();
        permission.setModule("authorisation");
        assertEquals("authorisation", permission.getModule());
    }

    @DisplayName("Permission entity can be fully constructed with module field")
    @Test
    void testPermissionEntityWithModule() {
        Permission permission = new Permission();
        permission.setModule("special");
        permission.setCode("READ_TRANSFER");
        permission.setEntityName("TRANSFER");
        permission.setActionName("READ");
        permission.setCanMakerChecker(false);

        assertEquals("special", permission.getModule());
        assertEquals("READ_TRANSFER", permission.getCode());
        assertEquals("TRANSFER", permission.getEntityName());
        assertEquals("READ", permission.getActionName());
        assertFalse(permission.isCanMakerChecker());
    }

    @DisplayName("hasCode is case-insensitive")
    @Test
    void testHasCodeIsCaseInsensitive() {
        Permission permission = new Permission();
        permission.setCode("READ_TRANSFER");

        assertTrue(permission.hasCode("read_transfer"));
        assertTrue(permission.hasCode("READ_TRANSFER"));
        assertTrue(permission.hasCode("Read_Transfer"));
    }

    @DisplayName("Non-reserved-keyword columns are named normally")
    @Test
    void testOtherColumnsNotBacktickEscaped() throws NoSuchFieldException {
        Field codeField = Permission.class.getDeclaredField("code");
        Column codeColumn = codeField.getAnnotation(Column.class);
        assertNotNull(codeColumn);
        assertEquals("code", codeColumn.name());

        Field entityNameField = Permission.class.getDeclaredField("entityName");
        Column entityNameColumn = entityNameField.getAnnotation(Column.class);
        assertNotNull(entityNameColumn);
        assertEquals("entity_name", entityNameColumn.name());

        Field actionNameField = Permission.class.getDeclaredField("actionName");
        Column actionNameColumn = actionNameField.getAnnotation(Column.class);
        assertNotNull(actionNameColumn);
        assertEquals("action_name", actionNameColumn.name());
    }
}
