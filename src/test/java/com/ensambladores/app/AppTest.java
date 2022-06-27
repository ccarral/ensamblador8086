package com.ensambladores.app;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import com.ensambladores.compiler.*;

import org.antlr.v4.*;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * Unit test for simple App.
 */
public class AppTest {
    @Test
    public void testSuccesfulParse() {
        String s = "mov ah,1\n";
        ANTLRInputStream stream = new ANTLRInputStream(s);
        asm8086Lexer lex = new asm8086Lexer(stream);
        CommonTokenStream tokens = new CommonTokenStream(lex);
        asm8086Parser parser = new asm8086Parser(tokens);
        ParseTree tree = parser.prog();
    }
}
