package com.ensambladores.app;

import static org.junit.Assert.assertTrue;

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
            File f = new File("./plantilla2.asm");
            File fOut  = new File("./plantilla2.o");

            Path path = Paths.get("./plantilla2.asm");

            FileInputStream fileInput = new FileInputStream(f);
            FileOutputStream fileOut = new FileOutputStream(fOut);
            PrintStream fileOutPrint = new PrintStream(fileOut);

            File fux = null;
            List<String> lineas = Files.readAllLines(path);


            ANTLRInputStream stream = new ANTLRInputStream(fileInput);
            asm8086Lexer lex = new asm8086Lexer(stream);
            CommonTokenStream tokens = new CommonTokenStream(lex);
            asm8086Parser parser = new asm8086Parser(tokens);

            Asm8086ErrorListener errorListener = new Asm8086ErrorListener(System.err);

            parser.removeErrorListeners();
            parser.addErrorListener(errorListener);

            Analizador8086 listener = new Analizador8086();
            listener.setErrorListener(errorListener);
            listener.setOutFile(fileOutPrint);
            listener.setInput(stream);
            listener.setLineas(lineas.toArray(new String[0]));

            parser.addParseListener(listener);
            ParseTree tree = parser.prog();
            TablaSimbolos ts = listener.getTablaSimbolos();
            //if (errorListener.isPoisoned()) {
                System.out.print(ts.toString());
           // }
        } catch (Exception e) {
            e.printStackTrace();
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
