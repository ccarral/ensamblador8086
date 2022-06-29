package com.ensambladores;

import java.util.ArrayDeque;
import java.util.List;

import org.antlr.v4.runtime.tree.TerminalNode;

import com.ensambladores.compiler.asm8086BaseListener;
import com.ensambladores.compiler.asm8086Parser.DbContext;
import com.ensambladores.compiler.asm8086Parser.DupdeclContext;
import com.ensambladores.compiler.asm8086Parser.DwContext;
import com.ensambladores.compiler.asm8086Parser.NumberContext;

public class Analizador8086 extends asm8086BaseListener {
    private int i = 0;
    private ArrayDeque dupDeclStack = new ArrayDeque<String>();

    @Override
    public void exitDupdecl(DupdeclContext ctx) {
        super.exitDupdecl(ctx);
        List<NumberContext> literal = ctx.number();
        for (NumberContext nContext : literal) {
            // System.out.println(nContext.SIGN());
            // System.out.println(nContext.NUMBER());
        }
        // System.out.println("ADIOS");
    }

    @Override
    public void exitDb(DbContext ctx) {
        super.exitDb(ctx);
        // Verificamos si estamos en contexto dup(byte)
        DupdeclContext dupDeclCtx = ctx.dupdecl();
        if (dupDeclCtx != null) {
            // Verificamos que la expresión sea del tamaño adecuado, es decir:
            // * Un solo caracter (esto se debería de validar en el análisis léxico)
            // * Un número entre 0x00 y 0xFF
            TerminalNode c = dupDeclCtx.CHAR();

            // Esto corresponde al segundo número que aparece en la regla,
            // es nulo si se utilizó un caracter
            NumberContext numCtx = dupDeclCtx.number(1);
            if (numCtx != null) {
                System.out.println(numCtx.getText());
            }
        }
    }

    @Override
    public void exitDw(DwContext ctx) {
        super.exitDw(ctx);
        // DupdeclContext dupDeclCtx = ctx.dupdecl();
        // System.out.println(dupDeclCtx.number().get(0).NUMBER());
    }

    public int parseNumberLiteral(String literal) {
        char atEnd = literal.charAt(literal.length() - 1);
        String substring = literal.substring(0, literal.length() - 1);
        switch (atEnd) {
            case 'H':
            case 'h':
                return Integer.parseInt(substring, 16);
            case 'B':
            case 'b':
                return Integer.parseInt(substring, 2);
            default:
                return Integer.parseInt(literal);
        }
    }
}
