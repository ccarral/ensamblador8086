package com.ensambladores;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.HexFormat;
import java.util.List;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.misc.Interval;

import com.google.common.primitives.UnsignedBytes;
import com.ensambladores.compiler.asm8086BaseListener;
import com.ensambladores.compiler.asm8086Parser.DbContext;
import com.ensambladores.compiler.asm8086Parser.DupdeclContext;
import com.ensambladores.compiler.asm8086Parser.DwContext;
import com.ensambladores.compiler.asm8086Parser.ExpressionlistContext;
import com.ensambladores.compiler.asm8086Parser.InstructionContext;
import com.ensambladores.compiler.asm8086Parser.LabelContext;
import com.ensambladores.compiler.asm8086Parser.LineContext;
import com.ensambladores.compiler.asm8086Parser.NameContext;
import com.ensambladores.compiler.asm8086Parser.NumberContext;
import com.ensambladores.error.Asm8086ErrorListener;
import com.ensambladores.instrucciones.Codificador;
import com.ensambladores.instrucciones.OpCode;
import com.ensambladores.instrucciones.OpCodeProperties;
import com.ensambladores.sym.TablaSimbolos;
import com.ensambladores.sym.TipoSimbolo;

public class Analizador8086 extends asm8086BaseListener {
    private TablaSimbolos tablaSimbolos = new TablaSimbolos();
    private boolean symbolFlag = false;
    private Asm8086ErrorListener errorListener;
    private PrintStream outFile;
    private int contadorPrograma = 0x0;
    private ANTLRInputStream input;
    private String[] lineas;
    private ArrayDeque<String> currentCodedInstructionList = new ArrayDeque<>();
    private ArrayDeque<OpCode> opCodeReturnStack = new ArrayDeque<>();
    private ArrayDeque<Integer> opCodePropsReturnStack = new ArrayDeque<>();

    public void setLineas(String[] lineas) {
        this.lineas = lineas;
    }

    public ANTLRInputStream getInput() {
        return input;
    }

    public void setInput(ANTLRInputStream input) {
        this.input = input;
    }

    public PrintStream getOutFile() {
        return outFile;
    }

    public long incrementaContadorPrograma(int offset) {
        this.contadorPrograma += offset;
        return this.contadorPrograma;
    }

    public void setOutFile(PrintStream outFile) {
        this.outFile = outFile;
    }

    public void setErrorListener(Asm8086ErrorListener errorListener) {
        this.errorListener = errorListener;
    }

    @Override
    public void enterLine(LineContext ctx) {
        super.enterLine(ctx);
        outFile.printf("%08X  ", this.contadorPrograma);
    }

    @Override
    public void exitLine(LineContext ctx) {
        super.exitLine(ctx);

        String codedInst = "";
        String errMsg = null;

        int linea = ctx.start.getLine();
        int col = ctx.start.getCharPositionInLine();

        InstructionContext instructionContext = ctx.instruction();
        if (instructionContext != null) {
            OpCode opCode = this.opCodeReturnStack.pop();
            int props = this.opCodePropsReturnStack.pop();
            int expectedProps = opCode.getProps();

            if ((expectedProps & OpCodeProperties.NO_ARGS) != 0 // La instruccion no esperaba argumentos
                    && (props & OpCodeProperties.ARGS) != 0 // Y recibio argumentos
            ) {
                errMsg = String.format("la instruccion %s no acepta argumentos", opCode);
                this.errorListener.syntaxError(null, null, linea, col, errMsg, null);
            }

            codedInst = Codificador.codifica(opCode);
            for (String bits : this.currentCodedInstructionList) {
                codedInst = codedInst.concat(bits);
            }
            this.currentCodedInstructionList.clear();

        }
        outFile.printf("%s %s\n", lineas[linea - 1], codedInst);
    }

    @Override
    public void exitLabel(LabelContext ctx) {
        super.exitLabel(ctx);
        NameContext nameCtx = ctx.name();
        String etiqueta = nameCtx.getText();
        int linea = ctx.getStart().getLine();
        int col = ctx.getStart().getCharPositionInLine();
        if (etiqueta.length() > 10) {
            String msg = String.format("la etiqueta %s es demasiado larga", etiqueta);
            this.errorListener.syntaxError(null, null, linea, col, msg, null);
        } else {
            tablaSimbolos.añadeSimbolo(etiqueta, this.contadorPrograma);
            this.symbolFlag = true;
        }
    }

    @Override
    public void exitInstruction(InstructionContext ctx) {
        super.exitInstruction(ctx);

        int flags = 0;
        String opcString = ctx.start.getText();
        OpCode opc = fromOpcodeStr(opcString);
        this.opCodeReturnStack.push(opc);
        if (ctx.expressionlist() != null) {
            flags |= OpCodeProperties.ARGS;
        }
        opCodePropsReturnStack.push(flags);
    }

    public void procesaDefineVar(DupdeclContext dupDeclCtx, ExpressionlistContext exprListCtx,
            TipoSimbolo tipoSimbolo) {
        int offset = tipoSimbolo == TipoSimbolo.BYTE ? 1 : 2;
        // Verificamos si estamos en contexto dup(byte)

        // Verificamos si estamos en contexto expressionlist
        if (exprListCtx != null) {
            // Puede ser una cadena, caracter o un número
            String texto = exprListCtx.getText();
            int linea = exprListCtx.getStart().getLine();
            int columna = exprListCtx.getStart().getCharPositionInLine();
            String msg = null;

            if (isCadena(texto)) {
                // Se suma al CP el número de caracteres menos 2 por comillas
                this.incrementaContadorPrograma(texto.length() - 2);
            } else {
                // Verificar que el número cabe en el tipo de variable
                try {
                    // Esto solo debería de ser una expresion numerica, pero nunca se sabe...
                    if (testOverflow(texto, tipoSimbolo)) {
                        msg = String.format("no es posible definir un %s con el número %s", tipoSimbolo, texto);
                        this.errorListener.syntaxError(null, null, linea, columna, msg, null);
                    } else {
                        this.incrementaContadorPrograma(offset);
                    }
                } catch (Exception e) {
                    msg = String.format("expresión no soportada: %s", texto);
                    this.errorListener.syntaxError(null, null, linea, columna, msg, null);
                }
            }
        } else if (dupDeclCtx != null) {
            // Verificamos que la expresión sea del tamaño adecuado
            NumberContext repetidoCtx = dupDeclCtx.number(0);
            String repetidoStr = repetidoCtx.getText();

            // Num de veces que se repite el patron
            int repetido = parseNumberLiteral(repetidoStr);

            // Esto corresponde al segundo número que aparece en la regla,
            // es nulo si se utilizó un caracter
            NumberContext numCtx = dupDeclCtx.number(1);

            if (numCtx != null) {
                String literal = numCtx.getText();
                // Verificar que no hay overflow
                if (testOverflow(literal, tipoSimbolo)) {
                    int linea = numCtx.NUMBER().getSymbol().getLine();
                    int columna = numCtx.NUMBER().getSymbol().getCharPositionInLine();
                    String msg = String.format("no es posible definir un %s con el número %s", tipoSimbolo, literal);
                    this.errorListener.syntaxError(null, null, linea, columna, msg, null);
                } else {
                    // Añadir al PC
                    this.incrementaContadorPrograma(repetido * offset);
                }
            }
        }

    }

    @Override
    public void exitDb(DbContext ctx) {
        super.exitDb(ctx);

        this.tablaSimbolos.setTipoUltimoAñadido(TipoSimbolo.BYTE);

        // Verificamos si estamos en contexto dup(byte)
        DupdeclContext dupDeclCtx = ctx.dupdecl();

        // Verificamos si estamos en contexto expressionlist
        ExpressionlistContext exprListCtx = ctx.expressionlist();
        this.procesaDefineVar(dupDeclCtx, exprListCtx, TipoSimbolo.BYTE);

    }

    @Override
    public void exitDw(DwContext ctx) {
        super.exitDw(ctx);
        this.tablaSimbolos.setTipoUltimoAñadido(TipoSimbolo.WORD);

        // Verificamos si estamos en contexto dup(byte)
        DupdeclContext dupDeclCtx = ctx.dupdecl();

        // Verificamos si estamos en contexto expressionlist
        ExpressionlistContext exprListCtx = ctx.expressionlist();
        this.procesaDefineVar(dupDeclCtx, exprListCtx, TipoSimbolo.WORD);
    }

    // Regresa el número contenido en literal como un entero con signo
    public static int parseNumberLiteral(String literal) {
        char atEnd = literal.charAt(literal.length() - 1);
        String substring = literal.substring(0, literal.length() - 1);
        Integer parsed;
        switch (atEnd) {
            case 'H':
            case 'h':
                return Integer.parseInt(substring, 16);
            case 'B':
            case 'b':
                return Integer.parseInt(substring, 2);
            default:
                parsed = Integer.parseInt(literal);
                return parsed;
        }
    }

    public static boolean testOverflow(String literal, TipoSimbolo tipoSimbolo) {
        char atEnd = literal.charAt(literal.length() - 1);
        String substring = literal.substring(0, literal.length() - 1);
        int parsed;
        byte[] bytes = { 0x00, 0x00, 0x00, 0x00 };
        String pad;
        switch (atEnd) {
            case 'H':
            case 'h':
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
                if (tipoSimbolo == TipoSimbolo.BYTE) {
                    return !(parsed >= -128 && parsed <= 127);
                } else if (tipoSimbolo == TipoSimbolo.WORD) {
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

    public static boolean isCadena(String s) {
        boolean izq = s.charAt(0) == '"';
        boolean der = s.charAt(s.length() - 1) == '"';
        return izq && der;
    }

    public TablaSimbolos getTablaSimbolos() {
        return this.tablaSimbolos;
    }

    public static OpCode fromOpcodeStr(String opc) {
        switch (opc) {
            case "PUSHF":
                return OpCode.PUSHF;
            default:
                return OpCode.UNKNOWN;
        }
    }
}
