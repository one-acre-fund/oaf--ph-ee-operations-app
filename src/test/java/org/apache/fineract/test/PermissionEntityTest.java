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
 * MySQL 8.0 made {@code GROUPING} a reserved keyword, so the JPA {@code @Column}
 * annotation must use backtick-escaped column name ({@code `grouping`}) to generate
 * valid SQL on both MySQL 5.7 and 8.0.
 */
class PermissionEntityTest {

    @DisplayName("grouping field @Column name must be backtick-escaped for MySQL 8.0 reserved keyword")
    @Test
    void testGroupingColumnNameIsBacktickEscaped() throws NoSuchFieldException {
        // Arrange
        Field groupingField = Permission.class.getDeclaredField("grouping");
        Column columnAnnotation = groupingField.getAnnotation(Column.class);

        // Assert
        assertNotNull(columnAnnotation, "@Column annotation must be present on 'grouping' field");
        assertEquals("`grouping`", columnAnnotation.name(),
                "Column name must be backtick-escaped because 'grouping' is a reserved keyword in MySQL 8.0");
    }

    @DisplayName("grouping field @Column must be non-nullable with max length 45")
    @Test
    void testGroupingColumnConstraints() throws NoSuchFieldException {
        // Arrange
        Field groupingField = Permission.class.getDeclaredField("grouping");
        Column columnAnnotation = groupingField.getAnnotation(Column.class);

        // Assert
        assertNotNull(columnAnnotation);
        assertFalse(columnAnnotation.nullable(), "grouping column must be non-nullable");
        assertEquals(45, columnAnnotation.length(), "grouping column length must be 45");
    }

    @DisplayName("grouping getter and setter work correctly")
    @Test
    void testGroupingGetterSetter() {
        // Arrange
        Permission permission = new Permission();

        // Act
        permission.setGrouping("authorisation");

        // Assert
        assertEquals("authorisation", permission.getGrouping());
    }

    @DisplayName("Permission entity can be fully constructed with grouping field")
    @Test
    void testPermissionEntityWithGrouping() {
        // Arrange
        Permission permission = new Permission();
        permission.setGrouping("special");
        permission.setCode("READ_TRANSFER");
        permission.setEntityName("TRANSFER");
        permission.setActionName("READ");
        permission.setCanMakerChecker(false);

        // Assert
        assertEquals("special", permission.getGrouping());
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

    @DisplayName("Non-reserved-keyword columns do not have backtick escaping")
    @Test
    void testOtherColumnsNotBacktickEscaped() throws NoSuchFieldException {
        // Verify that only 'grouping' is backtick-escaped, not other columns
        Field codeField = Permission.class.getDeclaredField("code");
        Column codeColumn = codeField.getAnnotation(Column.class);
        assertNotNull(codeColumn);
        assertEquals("code", codeColumn.name(), "Non-reserved column 'code' should not be backtick-escaped");

        Field entityNameField = Permission.class.getDeclaredField("entityName");
        Column entityNameColumn = entityNameField.getAnnotation(Column.class);
        assertNotNull(entityNameColumn);
        assertEquals("entity_name", entityNameColumn.name(), "Non-reserved column 'entity_name' should not be backtick-escaped");

        Field actionNameField = Permission.class.getDeclaredField("actionName");
        Column actionNameColumn = actionNameField.getAnnotation(Column.class);
        assertNotNull(actionNameColumn);
        assertEquals("action_name", actionNameColumn.name(), "Non-reserved column 'action_name' should not be backtick-escaped");
    }
}
