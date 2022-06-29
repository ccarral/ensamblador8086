package com.ensambladores;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ensambladores.sym.TipoSimbolo;

public class Analizador8086Test {
    @Test
    public void testParseNumberLiteral() {
        assertFalse(Analizador8086.testOverflow("FFH", TipoSimbolo.BYTE));
        assertFalse(Analizador8086.testOverflow("FFFFH", TipoSimbolo.WORD));
        assertTrue(Analizador8086.testOverflow("1FFH", TipoSimbolo.BYTE));
        assertTrue(Analizador8086.testOverflow("1FFFFH", TipoSimbolo.WORD));

        assertFalse(Analizador8086.testOverflow("11111111b", TipoSimbolo.BYTE));
        assertFalse(Analizador8086.testOverflow("1111111111111111b", TipoSimbolo.WORD));
        assertTrue(Analizador8086.testOverflow("111111111b", TipoSimbolo.BYTE));
        assertTrue(Analizador8086.testOverflow("11111111111111111b", TipoSimbolo.BYTE));

        assertFalse(Analizador8086.testOverflow("-128", TipoSimbolo.BYTE));
        assertFalse(Analizador8086.testOverflow("127", TipoSimbolo.BYTE));
        assertFalse(Analizador8086.testOverflow("-32768", TipoSimbolo.WORD));
        assertFalse(Analizador8086.testOverflow("32767", TipoSimbolo.WORD));

        assertTrue(Analizador8086.testOverflow("-129", TipoSimbolo.BYTE));
        assertTrue(Analizador8086.testOverflow("128", TipoSimbolo.BYTE));
        assertTrue(Analizador8086.testOverflow("-32769", TipoSimbolo.BYTE));
        assertTrue(Analizador8086.testOverflow("32768", TipoSimbolo.BYTE));
    }

    @Test
    public void testWordOverflow() {
        // assertTrue(Analizador8086.wordOverflow(-32769));
    }
}
