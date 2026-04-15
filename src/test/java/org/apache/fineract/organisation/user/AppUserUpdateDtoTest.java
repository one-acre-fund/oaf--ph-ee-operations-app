/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.organisation.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive test class for AppUserUpdateDto
 */
class AppUserUpdateDtoTest {

    private AppUserUpdateDto appUserUpdateDto;

    @BeforeEach
    void setUp() {
        appUserUpdateDto = new AppUserUpdateDto();
    }

    @DisplayName("Default constructor creates instance with null fields")
    @Test
    void test_default_constructor() {
        AppUserUpdateDto dto = new AppUserUpdateDto();
        
        assertNotNull(dto);
        assertNull(dto.getFirstname());
        assertNull(dto.getLastname());
    }

    @DisplayName("setFirstname and getFirstname work correctly")
    @Test
    void test_firstname_getter_setter() {
        String firstname = "John";
        
        appUserUpdateDto.setFirstname(firstname);
        
        assertEquals(firstname, appUserUpdateDto.getFirstname());
    }

    @DisplayName("setLastname and getLastname work correctly")
    @Test
    void test_lastname_getter_setter() {
        String lastname = "Doe";
        
        appUserUpdateDto.setLastname(lastname);
        
        assertEquals(lastname, appUserUpdateDto.getLastname());
    }

    @DisplayName("Firstname can be set to null")
    @Test
    void test_firstname_null() {
        appUserUpdateDto.setFirstname("John");
        appUserUpdateDto.setFirstname(null);
        
        assertNull(appUserUpdateDto.getFirstname());
    }

    @DisplayName("Lastname can be set to null")
    @Test
    void test_lastname_null() {
        appUserUpdateDto.setLastname("Doe");
        appUserUpdateDto.setLastname(null);
        
        assertNull(appUserUpdateDto.getLastname());
    }

    @DisplayName("Firstname can be set to empty string")
    @Test
    void test_firstname_empty() {
        String emptyString = "";
        
        appUserUpdateDto.setFirstname(emptyString);
        
        assertEquals(emptyString, appUserUpdateDto.getFirstname());
    }

    @DisplayName("Lastname can be set to empty string")
    @Test
    void test_lastname_empty() {
        String emptyString = "";
        
        appUserUpdateDto.setLastname(emptyString);
        
        assertEquals(emptyString, appUserUpdateDto.getLastname());
    }

    @DisplayName("Both fields can be set independently")
    @Test
    void test_both_fields_independent() {
        String firstname = "Jane";
        String lastname = "Smith";
        
        appUserUpdateDto.setFirstname(firstname);
        appUserUpdateDto.setLastname(lastname);
        
        assertEquals(firstname, appUserUpdateDto.getFirstname());
        assertEquals(lastname, appUserUpdateDto.getLastname());
    }

    @DisplayName("Fields can handle special characters")
    @Test
    void test_special_characters() {
        String firstnameWithSpecial = "José";
        String lastnameWithSpecial = "O'Connor";
        
        appUserUpdateDto.setFirstname(firstnameWithSpecial);
        appUserUpdateDto.setLastname(lastnameWithSpecial);
        
        assertEquals(firstnameWithSpecial, appUserUpdateDto.getFirstname());
        assertEquals(lastnameWithSpecial, appUserUpdateDto.getLastname());
    }

    @DisplayName("Fields can handle long strings")
    @Test
    void test_long_strings() {
        String longFirstname = "A".repeat(100);
        String longLastname = "B".repeat(100);
        
        appUserUpdateDto.setFirstname(longFirstname);
        appUserUpdateDto.setLastname(longLastname);
        
        assertEquals(longFirstname, appUserUpdateDto.getFirstname());
        assertEquals(longLastname, appUserUpdateDto.getLastname());
    }

    @DisplayName("equals() method works correctly - same reference")
    @Test
    void test_equals_same_object() {
        AppUserUpdateDto sameReference = appUserUpdateDto;
        assertEquals(appUserUpdateDto, sameReference);
    }

    @DisplayName("equals() method works correctly - equal objects")
    @Test
    void test_equals_equal_objects() {
        AppUserUpdateDto dto1 = new AppUserUpdateDto();
        dto1.setFirstname("John");
        dto1.setLastname("Doe");
        
        AppUserUpdateDto dto2 = new AppUserUpdateDto();
        dto2.setFirstname("John");
        dto2.setLastname("Doe");
        
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @DisplayName("equals() method works correctly - different firstname")
    @Test
    void test_equals_different_firstname() {
        AppUserUpdateDto dto1 = new AppUserUpdateDto();
        dto1.setFirstname("John");
        dto1.setLastname("Doe");
        
        AppUserUpdateDto dto2 = new AppUserUpdateDto();
        dto2.setFirstname("Jane");
        dto2.setLastname("Doe");
        
        assertNotEquals(dto1, dto2);
    }

    @DisplayName("equals() method works correctly - different lastname")
    @Test
    void test_equals_different_lastname() {
        AppUserUpdateDto dto1 = new AppUserUpdateDto();
        dto1.setFirstname("John");
        dto1.setLastname("Doe");
        
        AppUserUpdateDto dto2 = new AppUserUpdateDto();
        dto2.setFirstname("John");
        dto2.setLastname("Smith");
        
        assertNotEquals(dto1, dto2);
    }

    @DisplayName("equals() method works correctly - null fields")
    @Test
    void test_equals_with_nulls() {
        AppUserUpdateDto dto1 = new AppUserUpdateDto();
        AppUserUpdateDto dto2 = new AppUserUpdateDto();
        
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @DisplayName("equals() method works correctly - one null field")
    @Test
    void test_equals_one_null_field() {
        AppUserUpdateDto dto1 = new AppUserUpdateDto();
        dto1.setFirstname("John");
        
        AppUserUpdateDto dto2 = new AppUserUpdateDto();
        dto2.setFirstname("John");
        dto2.setLastname(null);
        
        assertEquals(dto1, dto2);
    }

    @DisplayName("equals() method returns false for null object")
    @Test
    void test_equals_null_object() {
        appUserUpdateDto.setFirstname("John");
        assertNotEquals(null, appUserUpdateDto);
    }

    @DisplayName("equals() method returns false for different class")
    @Test
    void test_equals_different_class() {
        appUserUpdateDto.setFirstname("John");
        assertNotEquals("Not an AppUserUpdateDto", appUserUpdateDto);
    }

    @DisplayName("hashCode() method is consistent")
    @Test
    void test_hashcode_consistency() {
        appUserUpdateDto.setFirstname("John");
        appUserUpdateDto.setLastname("Doe");
        
        int hashCode1 = appUserUpdateDto.hashCode();
        int hashCode2 = appUserUpdateDto.hashCode();
        
        assertEquals(hashCode1, hashCode2);
    }

    @DisplayName("toString() method includes class name and field values")
    @Test
    void test_toString() {
        appUserUpdateDto.setFirstname("John");
        appUserUpdateDto.setLastname("Doe");
        
        String toString = appUserUpdateDto.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("AppUserUpdateDto"));
        assertTrue(toString.contains("John"));
        assertTrue(toString.contains("Doe"));
    }

    @DisplayName("toString() method handles null fields")
    @Test
    void test_toString_null_fields() {
        String toString = appUserUpdateDto.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("AppUserUpdateDto"));
    }

    @DisplayName("canEqual() method works correctly")
    @Test
    void test_can_equal() {
        AppUserUpdateDto other = new AppUserUpdateDto();
        
        assertTrue(appUserUpdateDto.canEqual(other));
        assertTrue(other.canEqual(appUserUpdateDto));
    }
}



