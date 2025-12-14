package org.apache.fineract.test;

import org.apache.fineract.config.security.service.RandomPasswordGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RandomPasswordGeneratorTest {

    @Test
    @DisplayName("generate() returns a string of twice the requested length per implementation")
    void generatesImplementationLength() {
        RandomPasswordGenerator gen8 = new RandomPasswordGenerator(8);
        String pwd8 = gen8.generate();
        assertNotNull(pwd8);
        assertEquals(16, pwd8.length());

        RandomPasswordGenerator gen32 = new RandomPasswordGenerator(32);
        String pwd32 = gen32.generate();
        assertNotNull(pwd32);
        assertEquals(64, pwd32.length());
    }

    @Test
    @DisplayName("generate() contains only characters from the allowed pool and lowercase letters")
    void usesAllowedCharacterSet() {
        RandomPasswordGenerator gen = new RandomPasswordGenerator(50);
        String pwd = gen.generate();
        assertNotNull(pwd);
        assertTrue(pwd.matches("[A-Za-z0-9!@#$%^&*]+"), "Password should contain only allowed characters (A-Za-z0-9!@#$%^&*)");
    }

    @Test
    @DisplayName("generate() produces different values across calls (basic randomness)")
    void producesDifferentAcrossCalls() {
        RandomPasswordGenerator gen = new RandomPasswordGenerator(16);
        String p1 = gen.generate();
        String p2 = gen.generate();
        if (p1.equals(p2)) {
            p2 = gen.generate();
        }
        assertNotEquals(p1, p2, "Consecutive passwords should not be identical");
    }

    @Test
    @DisplayName("generate() does not return empty string for positive length")
    void notEmptyForPositiveLength() {
        RandomPasswordGenerator gen = new RandomPasswordGenerator(1);
        String pwd = gen.generate();
        assertNotNull(pwd);
        assertFalse(pwd.isEmpty());
        assertEquals(2, pwd.length());
    }
}
