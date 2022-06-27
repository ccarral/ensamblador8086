package com.ensambladores.app;

import java.io.IOException;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.tree.*;
import com.ensambladores.compiler.*;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) {
        ANTLRFileStream file;
        try {
            file = new ANTLRFileStream(args[1]);
            asm8086Lexer lex = new asm8086Lexer(file);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
