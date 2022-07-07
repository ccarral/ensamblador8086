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
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.tree.*;

import com.ensambladores.Analizador8086;
import com.ensambladores.compiler.*;
import com.ensambladores.error.Asm8086ErrorListener;
import com.ensambladores.sym.TablaSimbolos;

import org.apache.commons.io.FilenameUtils;

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
            String tokensFileName = base.concat(".tokens");

            PrintStream outStream = new PrintStream(outFileName);

            PrintStream tokensFileStream = new PrintStream(tokensFileName);

            fileInputStream = new FileInputStream(fileNameArg);

            List<String> lineas = Files.readAllLines(filePath);

            ANTLRInputStream stream = new ANTLRInputStream(fileInputStream);
            asm8086Lexer lex = new asm8086Lexer(stream);
            CommonTokenStream tokens = new CommonTokenStream(lex);

            List<Token> tokenList = (List<Token>) lex.getAllTokens();
            lex.reset();

            asm8086Parser parser = new asm8086Parser(tokens);

            Vocabulary v = parser.getVocabulary();
            List<String> tokensFiltrados = new ArrayList<>();
            tokensFiltrados.add("EOF");
            tokensFiltrados.add("EOL");

            for(Token t: tokenList){
                int tokenType = t.getType();
                String symbol = v.getSymbolicName(tokenType);
                String literal = t.getText();
                if(symbol!= null && symbol.equals("EOL")){
                    System.out.println();
                }else{
                    if(!tokensFiltrados.contains(symbol)
                            && symbol !=null
                            && t.getChannel() != Token.HIDDEN_CHANNEL){
                        System.out.printf("%s %s ",literal, symbol);
                    }
                }
            }

            int sizeTokens = tokens.size();


            Asm8086ErrorListener errorListener = new Asm8086ErrorListener(System.err);

            parser.removeErrorListeners();
            parser.addErrorListener(errorListener);

            Analizador8086 listener = new Analizador8086();
            listener.setErrorListener(errorListener);
            listener.setOutFile(outStream);
            listener.setInput(stream);
            listener.setLineas(lineas.toArray(new String[0]));
            listener.setTokenOutFile(tokensFileStream);

            parser.addParseListener(listener);
            ParseTree tree = parser.prog();
            TablaSimbolos ts = listener.getTablaSimbolos();

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
