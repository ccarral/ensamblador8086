package com.ensambladores.app;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;

import com.ensambladores.Analizador8086;
import com.ensambladores.compiler.*;
import com.ensambladores.error.Asm8086ErrorListener;
import com.ensambladores.sym.TablaSimbolos;

import org.antlr.v4.*;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DiagnosticErrorListener;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * Unit test for simple App.
 */
public class AppTest {
    @Test
    public void testSuccesfulParse() {
        try {
            App.main(new String[]{"plantilla3.asm"});
        } catch (Exception e) {
            e.printStackTrace();
            fail("Hubo excepcion en la app");
        }
    }

    @Test
    public void testSuccesfulNumberParse() {
        ANTLRInputStream stream = new ANTLRInputStream("-255");
        asm8086Lexer lex = new asm8086Lexer(stream);
        CommonTokenStream tokens = new CommonTokenStream(lex);
        asm8086Parser parser = new asm8086Parser(tokens);
        ParseTree tree = parser.number();
    }
}
