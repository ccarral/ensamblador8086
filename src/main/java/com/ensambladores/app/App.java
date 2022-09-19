package com.ensambladores.app;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

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
        FileInputStream fileInputStream = null;
        String fileNameArg;
        try {
            if (args.length < 1) {
                Scanner scan = new Scanner(System.in);
                System.out.println("No se detectó un archivo.\nEscriba el nombre de un archivo para comenzar:");
                fileNameArg = scan.nextLine();
            }else{
                fileNameArg = args[0];
            }

            Path filePath = Paths.get(fileNameArg);
            String base = FilenameUtils.getBaseName(fileNameArg);
            String outFileName = base.concat(".o");

            PrintStream outStream = new PrintStream(outFileName);

            try{
                fileInputStream = new FileInputStream(fileNameArg);
            } catch (FileNotFoundException e) {
                System.out.println("No se pudo encontrar el archivo "+fileNameArg);
                System.exit(-1);
            }


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
            tokensFiltrados.add("ENDS");
            tokensFiltrados.add("DATA");
            tokensFiltrados.add("STACK");
            tokensFiltrados.add("CODE");

            for(Token t: tokenList){
                int tokenType = t.getType();
                String symbol = v.getSymbolicName(tokenType);
                String literal = t.getText();
                if(symbol!= null && symbol.equals("EOL")
                ){
                    System.out.println();
                }else{
                    if(!tokensFiltrados.contains(symbol)
                            && symbol !=null
                            && t.getChannel() != Token.HIDDEN_CHANNEL){
                        System.out.printf("%s [%s] ",literal, symbol);
                    }
                }
            }

            Asm8086ErrorListener errorListener = new Asm8086ErrorListener(System.err);

            parser.removeErrorListeners();
            parser.addErrorListener(errorListener);

            Analizador8086 listener = new Analizador8086();
            listener.setErrorListener(errorListener);
            listener.setOutFile(outStream);
            //listener.setOutFile(System.out);
            //listener.setOutFile(System.out);
            listener.setInput(stream);
            listener.setLineas(lineas.toArray(new String[0]));
            listener.setVocab(v);

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
