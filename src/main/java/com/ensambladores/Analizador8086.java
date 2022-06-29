package com.ensambladores;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import javax.xml.transform.ErrorListener;

import org.antlr.v4.runtime.tree.TerminalNode;

import com.google.common.primitives.Ints;
import com.google.common.primitives.UnsignedBytes;
import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedInts;
import com.ensambladores.compiler.asm8086BaseListener;
import com.ensambladores.compiler.asm8086Parser.DbContext;
import com.ensambladores.compiler.asm8086Parser.DupdeclContext;
import com.ensambladores.compiler.asm8086Parser.DwContext;
import com.ensambladores.compiler.asm8086Parser.ExpressionlistContext;
import com.ensambladores.compiler.asm8086Parser.LabelContext;
import com.ensambladores.compiler.asm8086Parser.NameContext;
import com.ensambladores.compiler.asm8086Parser.NumberContext;
import com.ensambladores.compiler.asm8086Parser.String_Context;
import com.ensambladores.error.Asm8086ErrorListener;
import com.ensambladores.sym.TablaSimbolos;
import com.ensambladores.sym.TipoSimbolo;

public class Analizador8086 extends asm8086BaseListener {
    private int i = 0;
    private ArrayDeque dupDeclStack = new ArrayDeque<String>();
    private TablaSimbolos tablaSimbolos = new TablaSimbolos();
    private boolean symbolFlag = false;
    private Asm8086ErrorListener errorListener;
    private static final UnsignedInteger BYTE_UPPER_BOUND = UnsignedInteger.fromIntBits(255);
    private static final UnsignedInteger BYTE_LOWER_BOUND = UnsignedInteger.fromIntBits(0);
    private static final UnsignedInteger WORD_UPPER_BOUND = UnsignedInteger.fromIntBits(65535);
    private static final UnsignedInteger WORD_LOWER_BOUND = UnsignedInteger.fromIntBits(0);

    public void setErrorListener(Asm8086ErrorListener errorListener) {
        this.errorListener = errorListener;
    }

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
    public void exitLabel(LabelContext ctx) {
        super.exitLabel(ctx);
        NameContext nameCtx = ctx.name();
        String etiqueta = nameCtx.getText();
        tablaSimbolos.añadeSimbolo(etiqueta);
        this.symbolFlag = true;
    }

    @Override
    public void exitDb(DbContext ctx) {
        super.exitDb(ctx);

        this.tablaSimbolos.setTipoUltimoAñadido(TipoSimbolo.BYTE);

        // Verificamos si estamos en contexto dup(byte)
        DupdeclContext dupDeclCtx = ctx.dupdecl();

        // Verificamos si estamos en contexto expressionlist
        ExpressionlistContext exprListCtx = ctx.expressionlist();
        if (exprListCtx != null) {
            // Puede ser una cadena, caracter o un número
            String texto = exprListCtx.getText();
            if (isCadena(texto)) {
                // Se suma al CP el número de caracteres
                this.tablaSimbolos.incrementaContador(texto.length() - 2);
            } else {
                // Verificar que el número cabe en un byte
                if (testOverflow(texto, TipoSimbolo.BYTE)) {
                    int linea = exprListCtx.getStart().getLine();
                    int columna = exprListCtx.getStart().getCharPositionInLine();
                    String msg = String.format("no es posible definir un byte con el número %s", texto);
                    this.errorListener.syntaxError(null, null, linea, columna, msg, null);
                }
            }
        } else if (dupDeclCtx != null) {
            // Verificamos que la expresión sea del tamaño adecuado, es decir:
            // * Un solo caracter (esto se debería de validar en el análisis léxico)
            // * Un número entre 0x00 y 0xFF

            NumberContext repetidoCtx = dupDeclCtx.number(0);
            String repetidoStr = repetidoCtx.getText();

            // Num de veces que se repite el patron
            int repetido = Integer.parseInt(repetidoStr);

            // Esto corresponde al segundo número que aparece en la regla,
            // es nulo si se utilizó un caracter
            NumberContext numCtx = dupDeclCtx.number(1);

            if (numCtx != null) {
                String literal = numCtx.getText();
                // Verificar que no hay overflow
                if (testOverflow(literal, TipoSimbolo.BYTE)) {
                    int linea = numCtx.NUMBER().getSymbol().getLine();
                    int columna = numCtx.NUMBER().getSymbol().getCharPositionInLine();
                    String msg = String.format("no es posible definir un byte con el número %s", literal);
                    this.errorListener.syntaxError(null, null, linea, columna, msg, null);
                } else {
                    // Añadir al PC
                    this.tablaSimbolos.incrementaContador(repetido * 1);
                }
            }
        }
    }

    @Override
    public void exitDw(DwContext ctx) {
        super.exitDw(ctx);
        // DupdeclContext dupDeclCtx = ctx.dupdecl();
        // System.out.println(dupDeclCtx.number().get(0).NUMBER());
    }

    // Regresa el número contenido en literal como un entero con signo
    public static int parseNumberLiteral(String literal) {
        char atEnd = literal.charAt(literal.length() - 1);
        String substring = literal.substring(0, literal.length() - 1);
        Integer parsed;
        byte[] bytes = { 0x00, 0x00, 0x00, 0x00 };
        boolean isNeg = false;
        String pad;
        switch (atEnd) {
            case 'H':
            case 'h':
                char firstHexByte = substring.toUpperCase().charAt(0);
                isNeg = (firstHexByte == '8'
                        || firstHexByte == 'C'
                        || firstHexByte == 'E'
                        || firstHexByte == 'F')
                        && (substring.length() == 2
                                || substring.length() == 8);
                pad = isNeg ? "F" : "0";

                // Padding
                while (substring.length() < 8) {
                    substring = pad.concat(substring);
                }
                // Truncar
                if (substring.length() > 8) {
                    substring = substring.substring(substring.length() - 8, substring.length());
                }
                bytes = HexFormat.of().parseHex(substring);
                break;
            case 'B':
            case 'b':
                isNeg = substring.charAt(0) == '1' && substring.length() % 8 == 0;
                pad = isNeg ? "1" : "0";

                // Padding
                while (substring.length() < 32) {
                    substring = pad.concat(substring);
                }

                // Truncar
                if (substring.length() > 32) {
                    substring = substring.substring(substring.length() - 32, substring.length());
                }

                for (int i = 0; i < substring.length() / 8; i += 8) {
                    String s = substring.substring(i, i + 8);
                    Byte b = UnsignedBytes.parseUnsignedByte(s, 2);
                    bytes[i / 8] = b;
                }
                break;
            default:
                parsed = Integer.parseInt(literal);
                return parsed;
        }

        ByteBuffer buf = ByteBuffer.wrap(bytes);
        Integer n = Ints.fromByteArray(bytes);
        System.out.printf("s:%s, v: %d\n", substring, n);
        return n;
    }

    public static boolean testOverflow(String literal, TipoSimbolo tipoSimbolo) {
        char atEnd = literal.charAt(literal.length() - 1);
        String substring = literal.substring(0, literal.length() - 1);
        int parsed;
        byte[] bytes = { 0x00, 0x00, 0x00, 0x00 };
        boolean isNeg = false;
        String pad;
        switch (atEnd) {
            case 'H':
            case 'h':
                char firstHexByte = substring.toUpperCase().charAt(0);
                isNeg = (firstHexByte == '8'
                        || firstHexByte == 'C'
                        || firstHexByte == 'E'
                        || firstHexByte == 'F')
                        && (substring.length() == 2
                                || substring.length() == 8);
                // pad = isNeg ? "F" : "0";
                pad = "0";

                // Padding
                while (substring.length() < 8) {
                    substring = pad.concat(substring);
                }
                // Truncar
                if (substring.length() > 8) {
                    substring = substring.substring(substring.length() - 8, substring.length());
                }
                bytes = HexFormat.of().parseHex(substring);
                break;
            case 'B':
            case 'b':
                isNeg = substring.charAt(0) == '1' && substring.length() % 8 == 0;
                // pad = isNeg ? "1" : "0";
                pad = "0";

                // Padding
                while (substring.length() < 32) {
                    substring = pad.concat(substring);
                }

                // Truncar
                if (substring.length() > 32) {
                    substring = substring.substring(substring.length() - 32, substring.length());
                }
                // System.out.println("subs:"+substring);

                for (int i = 0; i < substring.length(); i += 8) {
                    String s = substring.substring(i, i + 8);
                    // System.out.println("s:"+s);
                    Byte b = UnsignedBytes.parseUnsignedByte(s, 2);
                    bytes[i / 8] = b;
                }
                break;
            default:
                parsed = Integer.parseInt(literal);
                if(tipoSimbolo == TipoSimbolo.BYTE){
                    return !(parsed >= -128 && parsed <= 127);
                }else if(tipoSimbolo == TipoSimbolo.WORD){
                    return !(parsed >= -32768 && parsed <= 32767);
                }
                break;
        }
        /*
         * StringBuilder sb = new StringBuilder();
         * sb.append("[ ");
         * for (byte b : bytes) {
         * sb.append(String.format("0x%02X ", b));
         * }
         * sb.append("]");
         * System.out.printf("val:%s, bytes:%s\n" , literal, sb.toString());
         */
        if (tipoSimbolo == TipoSimbolo.BYTE) {
            return bytes[0] != 0x00 || bytes[1] != 0x00 || bytes[2] != 0x00;
        } else if (tipoSimbolo == TipoSimbolo.WORD) {
            return bytes[0] != 0x00 || bytes[1] != 00;
        }

        return true;

    }

    public static boolean byteOverflow(int num) {
        return num < 0 || num > 255;
    }

    public static boolean wordOverflow(int num) {
        return num < 0 || num > 65535;
    }

    public static boolean isCadena(String s) {
        boolean izq = s.charAt(0) == '"';
        boolean der = s.charAt(s.length() - 1) == '"';
        return izq && der;
    }

    public TablaSimbolos getTablaSimbolos() {
        int a = 0;
        return this.tablaSimbolos;
    }
}
