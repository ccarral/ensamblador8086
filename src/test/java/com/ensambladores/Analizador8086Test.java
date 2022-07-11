package com.ensambladores;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.ensambladores.sym.TamañoSimbolo;
import org.junit.Test;

public class Analizador8086Test {
    @Test
    public void testParseNumberLiteral() {
        assertFalse(Analizador8086.testOverflow("FFH", TamañoSimbolo.BYTE));
        assertFalse(Analizador8086.testOverflow("FFFFH", TamañoSimbolo.WORD));
        assertTrue(Analizador8086.testOverflow("1FFH", TamañoSimbolo.BYTE));
        assertTrue(Analizador8086.testOverflow("1FFFFH", TamañoSimbolo.WORD));

        assertFalse(Analizador8086.testOverflow("11111111b", TamañoSimbolo.BYTE));
        assertFalse(Analizador8086.testOverflow("1111111111111111b", TamañoSimbolo.WORD));
        assertTrue(Analizador8086.testOverflow("111111111b", TamañoSimbolo.BYTE));
        assertTrue(Analizador8086.testOverflow("11111111111111111b", TamañoSimbolo.BYTE));

        assertFalse(Analizador8086.testOverflow("-128", TamañoSimbolo.BYTE));
        assertFalse(Analizador8086.testOverflow("127", TamañoSimbolo.BYTE));
        assertFalse(Analizador8086.testOverflow("-32768", TamañoSimbolo.WORD));
        assertFalse(Analizador8086.testOverflow("32767", TamañoSimbolo.WORD));

        assertTrue(Analizador8086.testOverflow("-129", TamañoSimbolo.BYTE));
        assertTrue(Analizador8086.testOverflow("128", TamañoSimbolo.BYTE));
        assertTrue(Analizador8086.testOverflow("-32769", TamañoSimbolo.BYTE));
        assertTrue(Analizador8086.testOverflow("32768", TamañoSimbolo.BYTE));
    }

    @Test
    public void testWordOverflow() {
        // assertTrue(Analizador8086.wordOverflow(-32769));
    }
}
