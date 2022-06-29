package com.ensambladores;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class Analizador8086Test {
    @Test
    public void testParseNumberLiteral() {
        Analizador8086 a = new Analizador8086();
        assertEquals(0xFF, a.parseNumberLiteral("FFH"));
        assertEquals(0xFF, a.parseNumberLiteral("11111111b"));
        assertEquals(0xFF, a.parseNumberLiteral("255"));

    }
}
