package com.ensambladores.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.tree.*;

import com.ensambladores.Analizador8086;
import com.ensambladores.compiler.*;
import com.ensambladores.error.Asm8086ErrorListener;
import com.ensambladores.sym.TablaSimbolos;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.stringtemplate.v4.ST;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) {
        FileInputStream fileInputStream;
        try {
            if (args.length < 1) {
                System.err.println(ayudaStr());
                System.exit(-1);
            }

            String fileNameArg = args[0];
            Path filePath = Paths.get(fileNameArg);
            String base = FilenameUtils.getBaseName(fileNameArg);
            String outFileName = base.concat(".o");

            PrintStream outStream = new PrintStream(outFileName);

            fileInputStream = new FileInputStream(fileNameArg);

            List<String> lineas = Files.readAllLines(filePath);

            ANTLRInputStream stream = new ANTLRInputStream(fileInputStream);
            asm8086Lexer lex = new asm8086Lexer(stream);
            CommonTokenStream tokens = new CommonTokenStream(lex);
            asm8086Parser parser = new asm8086Parser(tokens);

            Asm8086ErrorListener errorListener = new Asm8086ErrorListener(System.err);

            parser.removeErrorListeners();
            parser.addErrorListener(errorListener);

            Analizador8086 listener = new Analizador8086();
            listener.setErrorListener(errorListener);
            listener.setOutFile(outStream);
            listener.setInput(stream);
            listener.setLineas(lineas.toArray(new String[0]));

            parser.addParseListener(listener);
            ParseTree tree = parser.prog();
            TablaSimbolos ts = listener.getTablaSimbolos();

            if (errorListener.isLongTermPoisoned()) {
                System.err.println("\nHubo errores en la compilación!");
                System.err.println("\nNota: los símbolos son agregados a la tabla antes de verificar que estan");
                System.err.println("       correctamente declarados, por lo que sus direcciones pueden ser equivocadas.");
                System.err.println("       Para ver una tabla correcta, corrija los errores en la compilación\n");
            } 
              
            System.out.println(ts.toString());
            

            try (BufferedReader br = new BufferedReader(new FileReader(outFileName))) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
            }
        } catch (IOException e) {
            System.err.printf("error fatal al abrir el archivo: %s\n", e.getMessage());
            System.exit(-2);
        }
    }

    private static String ayudaStr() {
        String ayuda = String.format("Uso del programa:\n");
        ayuda = ayuda.concat("java -jar ensamblador8086.jar programa.asm\n");
        return ayuda;
    }
}
