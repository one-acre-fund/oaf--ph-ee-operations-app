package org.apache.fineract.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.fineract.organisation.permission.PermissionData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PermissionData}, including the {@code @JsonProperty("grouping")}
 * annotation that preserves API backward compatibility after the DB column rename
 * from {@code grouping} to {@code module}.
 */
class PermissionDataTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DisplayName("JSON serialization outputs 'grouping' (not 'module') for backward compatibility")
    @Test
    void testJsonSerializationUsesGroupingKey() throws Exception {
        PermissionData data = new PermissionData("authorisation", "READ_USER", "USER", "READ", true);

        String json = objectMapper.writeValueAsString(data);

        assertTrue(json.contains("\"grouping\""), "JSON must contain 'grouping' key for API compatibility");
        assertFalse(json.contains("\"module\""), "JSON must NOT contain 'module' key — internal field name should not leak to API");
        assertTrue(json.contains("\"authorisation\""));
    }

    @DisplayName("JSON deserialization accepts 'grouping' key and maps to module field")
    @Test
    void testJsonDeserializationAcceptsGroupingKey() throws Exception {
        String json = "{\"grouping\":\"transfers\",\"code\":\"READ_TRANSFER\",\"entityName\":\"TRANSFER\",\"actionName\":\"READ\",\"selected\":true}";

        PermissionData data = objectMapper.readValue(json, PermissionData.class);

        assertEquals("transfers", data.getModule());
        assertEquals("READ_TRANSFER", data.getCode());
        assertEquals("TRANSFER", data.getEntityName());
        assertEquals("READ", data.getActionName());
        assertTrue(data.getSelected());
    }

    @DisplayName("Constructor sets all fields correctly")
    @Test
    void testConstructor() {
        PermissionData data = new PermissionData("authorisation", "CREATE_USER", "USER", "CREATE", false);

        assertEquals("authorisation", data.getModule());
        assertEquals("CREATE_USER", data.getCode());
        assertEquals("USER", data.getEntityName());
        assertEquals("CREATE", data.getActionName());
        assertFalse(data.getSelected());
    }

    @DisplayName("from() factory sets code and selected, others null")
    @Test
    void testFromFactory() {
        PermissionData data = PermissionData.from("READ_TRANSFER", true);

        assertNull(data.getModule());
        assertEquals("READ_TRANSFER", data.getCode());
        assertNull(data.getEntityName());
        assertNull(data.getActionName());
        assertTrue(data.getSelected());
    }

    @DisplayName("instance() factory creates PermissionData correctly")
    @Test
    void testInstanceFactory() {
        PermissionData data = PermissionData.instance("operations", "REFUND", "TRANSFER", "REFUND", false);

        assertEquals("operations", data.getModule());
        assertEquals("REFUND", data.getCode());
        assertEquals("TRANSFER", data.getEntityName());
        assertEquals("REFUND", data.getActionName());
        assertFalse(data.getSelected());
    }

    @DisplayName("Setters update fields correctly")
    @Test
    void testSetters() {
        PermissionData data = new PermissionData();

        data.setModule("authorisation");
        data.setCode("UPDATE_ROLE");
        data.setEntityName("ROLE");
        data.setActionName("UPDATE");
        data.setSelected(true);

        assertEquals("authorisation", data.getModule());
        assertEquals("UPDATE_ROLE", data.getCode());
        assertEquals("ROLE", data.getEntityName());
        assertEquals("UPDATE", data.getActionName());
        assertTrue(data.getSelected());
    }
}

