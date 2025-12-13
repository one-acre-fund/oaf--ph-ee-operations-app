package org.apache.fineract.test;

import org.apache.fineract.config.security.service.RandomPasswordGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RandomPasswordGeneratorTest {

    @Test
    @DisplayName("generate() returns a string of the requested length")
    void generatesRequestedLength() {
        RandomPasswordGenerator gen8 = new RandomPasswordGenerator(8);
        String pwd8 = gen8.generate();
        assertNotNull(pwd8);
        assertEquals(8, pwd8.length());

        RandomPasswordGenerator gen32 = new RandomPasswordGenerator(32);
        String pwd32 = gen32.generate();
        assertNotNull(pwd32);
        assertEquals(32, pwd32.length());
    }

    @Test
    @DisplayName("generate() uses only lowercase alphabetic characters a-z")
    void usesOnlyLowercaseLetters() {
        RandomPasswordGenerator gen = new RandomPasswordGenerator(50);
        String pwd = gen.generate();
        assertTrue(pwd.matches("[a-z]+"), "Password should contain only lowercase letters a-z");
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
    }
}

