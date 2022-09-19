package com.ensambladores;

import java.io.PrintStream;
import java.util.*;

import com.ensambladores.compiler.asm8086Parser;
import com.ensambladores.instrucciones.*;
import com.ensambladores.sym.TamañoSimbolo;
import com.ensambladores.sym.TipoSimbolo;
import com.google.common.primitives.Ints;
import org.antlr.v4.runtime.ANTLRInputStream;

import com.ensambladores.compiler.asm8086BaseListener;
import com.ensambladores.compiler.asm8086Parser.DbContext;
import com.ensambladores.compiler.asm8086Parser.DupdeclContext;
import com.ensambladores.compiler.asm8086Parser.DwContext;
import com.ensambladores.compiler.asm8086Parser.EquContext;
import com.ensambladores.compiler.asm8086Parser.ExpressionlistContext;
import com.ensambladores.compiler.asm8086Parser.InstructionContext;
import com.ensambladores.compiler.asm8086Parser.LabelContext;
import com.ensambladores.compiler.asm8086Parser.LineContext;
import com.ensambladores.compiler.asm8086Parser.NameContext;
import com.ensambladores.compiler.asm8086Parser.NumberContext;
import com.ensambladores.compiler.asm8086Parser.ExpressionContext;
import com.ensambladores.compiler.asm8086Parser.Register_Context;
import com.ensambladores.error.Asm8086ErrorListener;
import com.ensambladores.sym.Simbolo;
import com.ensambladores.sym.TablaSimbolos;
import com.google.common.base.Strings;
import com.google.common.primitives.UnsignedBytes;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;

public class Analizador8086 extends asm8086BaseListener {
    private Simbolo simboloPendiente = null;
    private TablaSimbolos tablaSimbolos = new TablaSimbolos();
    private Asm8086ErrorListener errorListener;
    private PrintStream outFile;
    private int contadorPrograma = 0x0;
    private ANTLRInputStream input;
    private String[] lineas;
    private ArrayDeque<OpCode> opCodeReturnStack = new ArrayDeque<>();
    private ArrayDeque<Integer> opCodePropsReturnStack = new ArrayDeque<>();
    private int longestLineLength = -99999;
    // No se espera leer o escribir más de 16 bytes por vez
    private byte[] outByteBuf = new byte[200];
    private int bufWriteIdx = 0;

    private Registro detectedReg1 = Registro.AX;
    private Registro detectedReg2;
    private int detectedMem1 = 0x0000;
    private int detectedMem2 = 0x0000;

    private String detectedInmStr = "00000000";

    private byte detectedDisp8;
    private byte[] detectedDisp16;
    private MMMField detectedMMMBits = MMMField.MMM111;
    private OOModifierBits detectedOOBits = OOModifierBits.OO11;

    private TamañoSimbolo detectedW = TamañoSimbolo.BYTE;

    public void setVocab(Vocabulary vocab) {
        this.vocab = vocab;
    }

    private Vocabulary vocab;

    public void setLineas(String[] lineas) {
        for (int i = 0; i < lineas.length; i++) {
            // Elimina caracteres extraños
            String linea = lineas[i].trim().replaceAll("\t", "");
            ;
            lineas[i] = linea;
            if (linea.length() > this.longestLineLength) {
                this.longestLineLength = linea.length();
            }
        }
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
        outFile.printf("%04X  ", this.contadorPrograma);
    }

    @Override
    public void exitLine(LineContext ctx) {
        super.exitLine(ctx);

        String codedInst = "OK";

        int linea = ctx.start.getLine();

        if (this.errorListener.isPoisoned()) {
            codedInst = this.errorListener.getErrMsgBuf();
            this.errorListener.setPoisoned(false);
            this.errorListener.flushErrMsgBuf();
            this.clearOutBuf();
        } else {
            this.contadorPrograma += this.bufWriteIdx;
            // System.out.println("Bufidx " + this.bufWriteIdx);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < this.bufWriteIdx; i++) {
                // System.out.println("hex: "+ String.format("%02X", this.outByteBuf[i]));
                sb = sb.append(String.format("%02X", this.outByteBuf[i]));
                // System.out.println("sb:"+sb);
            }
            // System.out.println(sb);
            if (!sb.toString().isEmpty()) {
                codedInst = sb.toString();
            }
            this.clearOutBuf();

            // Añadir símbolo pendiente
            if (this.simboloPendiente != null) {
                this.tablaSimbolos.añadeSimbolo(this.simboloPendiente);
                this.simboloPendiente = null;
            }
        }
        // System.err.println(lineas[linea - 1]);

        outFile.printf("%s %s\n", Strings.padEnd(lineas[linea - 1], this.longestLineLength, ' '),
                Strings.padStart(codedInst, 8, ' '));
    }

    @Override
    public void exitLabel(LabelContext ctx) {
        super.exitLabel(ctx);
        NameContext nameCtx = ctx.name();
        String etiqueta = nameCtx.getText().toLowerCase();
        int linea = ctx.getStart().getLine();
        int col = ctx.getStart().getCharPositionInLine();
        if (etiqueta.length() > 10) {
            String msg = String.format("la etiqueta %s es demasiado larga", etiqueta);
            this.errorListener.syntaxError(null, null, linea, col, msg, null);
        } else {
            this.simboloPendiente = new Simbolo(etiqueta);
            this.simboloPendiente.setDireccion(this.contadorPrograma);
        }
    }

    @Override
    public void exitInstruction(InstructionContext ctx) {
        super.exitInstruction(ctx);
        AddressingMode addressingMode;
        EnumSet<AddressingMode> supportedAddressingModes;
        String errMsg = null;
        int linea = ctx.start.getLine();
        int col = ctx.start.getCharPositionInLine();
        byte[] disp = {};
        byte[] inm = {};

        String opcString = ctx.opcode().INST().getText();
        OpCode opCode = fromOpcodeStr(opcString);

        // System.out.println("Opcode detectado:"+opCode);

        ExpressionlistContext exprListCtx = ctx.expressionlist();
        if (exprListCtx == null) {
            addressingMode = AddressingMode.IMPL;
        } else {
            // Obtener el modo de direccionamiento de la linea
            exprListCtx.expression();
            addressingMode = this.getAddressingMode(exprListCtx);
        }

        // System.out.println("A ver que hay!:" +
        // Arrays.toString(this.opCodeReturnStack.toArray()));
        // OpCode opCode = this.opCodeReturnStack.pop();

        supportedAddressingModes = opCode.getAddressingModes();

        // Valida la lógica de la instrucción
        // System.out.println("detectado:"+opCode);
        if (opCode != OpCode.UNKNOWN) {
            if (!supportedAddressingModes.contains(addressingMode)) {
                errMsg = String.format("modo de direccionamiento no soportado: %s", addressingMode);
                this.errorListener.syntaxError(null, null, linea, col, errMsg, null);
            } else {
                boolean esSaltoLargo = false;

                if (opCode == OpCode.LOOPNE) {
                    // Calcular salto entre contador de programa actual y etiqueta destino
                    // Si esta no existe, entonces el error ya se detectó.
                    if (!this.errorListener.isPoisoned()) {
                        disp = Codificador.codificaSalto(this.contadorPrograma, this.detectedMem1);
                        // El salto NO debería de ser mayor a un byte
                    }
                }
                if (EnumSet.of(OpCode.JNAE, OpCode.JP, OpCode.JC, OpCode.JNL, OpCode.JAE).contains(opCode)) {
                    // Calcular salto
                    // Ya se debe de tener la direccion de la etq en memoria
                    esSaltoLargo = Codificador.esSaltoLargo(this.contadorPrograma, this.detectedMem1);
                    disp = Codificador.codificaSalto(this.contadorPrograma, this.detectedMem1);
                }

                InstructionTemplate template = Codificador.getTemplate(opCode, addressingMode, esSaltoLargo);
                if (EnumSet.of(AddressingMode.REG8, AddressingMode.REG16, AddressingMode.MEM)
                        .contains(addressingMode)) {
                    template.setRrr(detectedReg1.getRRRBits());
                }

                template.setW(detectedW);
                template.setMmm(detectedMMMBits);
                template.setOoo(detectedOOBits);

                if (addressingMode == AddressingMode.REG_MEM) {
                    disp = Codificador.codificaSalto(this.contadorPrograma, this.detectedMem2);
                }else if(addressingMode == AddressingMode.MEM){
                    // Si el operando es memoria directa, calcula salto
                    if(detectedMMMBits == MMMField.MMM110 && detectedOOBits == OOModifierBits.OO00){
                        disp = Codificador.codificaSalto(this.contadorPrograma, detectedMem1);
                    }else{
                        if(detectedOOBits == OOModifierBits.OO01){
                            disp = new byte[]{detectedDisp8};
                        }else if(detectedOOBits == OOModifierBits.OO10){
                            disp = detectedDisp16;
                        }
                    }
                }else if(addressingMode == AddressingMode.REG_INM){
                    if(!testOverflow(detectedInmStr, TamañoSimbolo.BYTE)){
                        byte inmByte = (byte)(parseNumberLiteral(detectedInmStr)&0xFF);
                        inm = new byte[]{inmByte};
                    }else{
                        inm = codificaDisp16(parseNumberLiteral(detectedInmStr));
                    }
                }

                byte[] instBytes = template.buildBytes();
                // System.out.println(Arrays.toString(instBytes));
                this.writeAtEndOfBuffer(instBytes);
                //System.out.println("opc bytes:" + Arrays.toString(instBytes));
                //System.out.println("disp:" + Arrays.toString(disp));
                this.writeAtEndOfBuffer(disp);

                this.writeAtEndOfBuffer(inm);
            }
        } else {
            errMsg = String.format("inst no reconocida: %s", opcString);
            this.errorListener.syntaxError(null, null, linea, col, errMsg, null);
        }

    }

    // @Override
    // public void exitOpcode(OpcodeContext ctx) {
    // super.exitOpcode(ctx);
    // // Añadir al return stack
    // String opcString = ctx.INST().getText();
    // OpCode opc = fromOpcodeStr(opcString);
    // System.out.println("Detectado:" + opc);
    // System.out.printf("Entrando aqui!: %s\n",
    // Arrays.toString(this.opCodeReturnStack.toArray()));
    // this.opCodeReturnStack.push(opc);
    // }

    public void procesaDefineVar(DupdeclContext dupDeclCtx, ExpressionlistContext exprListCtx,
            TamañoSimbolo tamañoSimbolo) {
        int offset = tamañoSimbolo == TamañoSimbolo.BYTE ? 1 : 2;
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
                if (this.simboloPendiente != null) {
                    this.simboloPendiente.setLongitud(texto.length() - 2);
                }
            } else {
                // Verificar que el número cabe en el tipo de variable
                try {
                    // Esto solo debería de ser una expresion numerica, pero nunca se sabe...
                    if (testOverflow(texto, tamañoSimbolo)) {
                        msg = String.format("no es posible definir un %s con el número %s", tamañoSimbolo, texto);
                        this.errorListener.syntaxError(null, null, linea, columna, msg, null);
                    } else {
                        this.incrementaContadorPrograma(offset);
                        if (this.simboloPendiente != null) {
                            this.simboloPendiente.setLongitud(offset);
                        }
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
                if (testOverflow(literal, tamañoSimbolo)) {
                    int linea = numCtx.NUM().getSymbol().getLine();
                    int columna = numCtx.NUM().getSymbol().getCharPositionInLine();
                    String msg = String.format("no es posible definir un %s con el número %s", tamañoSimbolo, literal);
                    this.errorListener.syntaxError(null, null, linea, columna, msg, null);
                } else {
                    // Añadir al PC
                    this.incrementaContadorPrograma(repetido * offset);
                    if (this.simboloPendiente != null) {
                        this.simboloPendiente.setLongitud(repetido * offset);
                    }
                }
            } else {
                // Asumimos que se usó un caracter
                this.incrementaContadorPrograma(repetido * offset);
                if (this.simboloPendiente != null) {
                    this.simboloPendiente.setLongitud(repetido * offset);
                }
            }
        }
    }

    // @Override
    // public void exitExpressionlist(ExpressionlistContext exprListCtx) {
    // super.exitExpressionlist(exprListCtx);
    // int props = 0;
    // props |= OpCodeProperties.ARGS;
    // if (exprListCtx.children.size() == 1) {
    // String cadena = exprListCtx.getText();
    // Simbolo sim = this.tablaSimbolos.getSimb(cadena);
    // if (sim != null) {
    // props |= OpCodeProperties.ARG_IS_LABEL;
    // byte[] jumpAddr = Codificador.codificaSalto(this.contadorPrograma,
    // sim.getDireccion());
    // this.writeAtEndOfBuffer(jumpAddr);
    // OpCode opc;
    // if ((opc = this.opCodeReturnStack.peekFirst()) != null &&
    // Codificador.noSoportado(opc)) {
    // this.clearOutBuf();
    // }
    // }
    // } else if (exprListCtx.children.size() == 2) {
    // props |= OpCodeProperties.ARGS_2;
    // }
    // this.opCodePropsReturnStack.push(props);
    // }

    @Override
    public void exitDb(DbContext ctx) {
        super.exitDb(ctx);

        if (this.simboloPendiente != null) {
            simboloPendiente.setTam(TamañoSimbolo.BYTE);
            simboloPendiente.setTipoSimbolo(TipoSimbolo.VAR);
        }

        // Verificamos si estamos en contexto dup(byte)
        DupdeclContext dupDeclCtx = ctx.dupdecl();

        // Verificamos si estamos en contexto expressionlist
        ExpressionlistContext exprListCtx = ctx.expressionlist();
        this.procesaDefineVar(dupDeclCtx, exprListCtx, TamañoSimbolo.BYTE);

    }

    @Override
    public void exitDw(DwContext ctx) {
        super.exitDw(ctx);

        if (this.simboloPendiente != null) {
            simboloPendiente.setTam(TamañoSimbolo.WORD);
            simboloPendiente.setTipoSimbolo(TipoSimbolo.VAR);
        }

        // Verificamos si estamos en contexto dup(byte)
        DupdeclContext dupDeclCtx = ctx.dupdecl();

        // Verificamos si estamos en contexto expressionlist
        ExpressionlistContext exprListCtx = ctx.expressionlist();
        this.procesaDefineVar(dupDeclCtx, exprListCtx, TamañoSimbolo.WORD);
    }

    @Override
    public void exitEnds(asm8086Parser.EndsContext ctx) {
        super.exitEnds(ctx);
        this.contadorPrograma = 0;
    }

    @Override
    public void exitDupdecl(DupdeclContext ctx) {
        super.exitDupdecl(ctx);
    }

    @Override
    public void exitRegister_(Register_Context ctx) {
        super.exitRegister_(ctx);
    }

    @Override
    public void exitNumber(NumberContext ctx) {
        super.exitNumber(ctx);
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

    public static boolean testOverflow(String literal, TamañoSimbolo tamañoSimbolo) {
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
                if (tamañoSimbolo == TamañoSimbolo.BYTE) {
                    return !(parsed >= -128 && parsed <= 127);
                } else if (tamañoSimbolo == TamañoSimbolo.WORD) {
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
         * sb.append"]");
         * default_.out.printf("val:%s, bytes:%s\n" , literal, sb.toString());
         */
        if (tamañoSimbolo == TamañoSimbolo.BYTE) {
            return bytes[0] != 0x00 || bytes[1] != 0x00 || bytes[2] != 0x00;
        } else if (tamañoSimbolo == TamañoSimbolo.WORD) {
            return bytes[0] != 0x00 || bytes[1] != 00;
        }

        return true;

    }

    @Override
    public void exitEqu(EquContext ctx) {
        super.exitEqu(ctx);
        String valor = ctx.expression().getText();
        int linea = ctx.getStart().getLine();
        int col = ctx.getStart().getCharPositionInLine();
        String msg;
        if (this.simboloPendiente == null) {
            msg = "nombre de constante faltante";
            this.errorListener.syntaxError(null, null, linea, col, msg, null);
        } else {
            this.simboloPendiente.setTipoSimbolo(TipoSimbolo.CONST);
            this.simboloPendiente.setTam(TamañoSimbolo.WORD);
        }

        if (testOverflow(valor, TamañoSimbolo.WORD)) {
            msg = String.format("no es posible definir un %s con el número %s", TamañoSimbolo.WORD, valor);
            this.errorListener.syntaxError(null, null, linea, col, msg, null);
        } else {
            this.contadorPrograma += 2;
        }
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
            case "DAA":
                return OpCode.DAA;
            case "STOSB":
                return OpCode.STOSB;
            case "LODSB":
                return OpCode.LODSB;
            case "CBW":
                return OpCode.CBW;
            case "POPF":
                return OpCode.POPF;
            case "DEC":
                return OpCode.DEC;
            case "POP":
                return OpCode.POP;
            case "NEG":
                return OpCode.NEG;
            case "IDIV":
                return OpCode.IDIV;
            case "XOR":
                return OpCode.XOR;
            case "LES":
                return OpCode.LES;
            case "SHL":
                return OpCode.SHL;
            case "MOV":
                return OpCode.MOV;
            case "LOOPNE":
                return OpCode.LOOPNE;
            case "JNAE":
                return OpCode.JNAE;
            case "JP":
                return OpCode.JP;
            case "JC":
                return OpCode.JC;
            case "JNL":
                return OpCode.JNL;
            case "JAE":
                return OpCode.JAE;
            default:
                return OpCode.UNKNOWN;
        }
    }

    public void writeAtEndOfBuffer(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            this.outByteBuf[this.bufWriteIdx + i] = bytes[i];
        }
        this.bufWriteIdx += bytes.length;
    }

    public void printOutBuf() {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ");
        for (int i = 0; i < this.bufWriteIdx; i++) {
            byte b = this.outByteBuf[i];
            sb.append(String.format("0x%02X ", b));
        }
        sb.append("]");
        // System.out.printf("%s\n", sb.toString());
    }

    public void clearOutBuf() {
        this.bufWriteIdx = 0;
    }

    public AddressingMode getAddressingMode(ExpressionlistContext ctx) {
        List<asm8086Parser.ExpressionContext> expressions = ctx.expression();
        String errMsg = null;
        int linea = ctx.getStart().getLine();
        int col = ctx.getStart().getCharPositionInLine();
        if (expressions.size() == 1) {
            // Significa que NO hay una coma en la lista de argumentos
            ExpressionContext exprCtx = expressions.get(0);
            // List<MultiplyingExpressionContext> mpexctxList =
            // exprCtx.multiplyingExpression();
            List<asm8086Parser.ArgumentContext> arguments = exprCtx.argument();

            // Por lo pronto vamos a asumir que solo hay un argumento en la lista de
            // argumentos
            if (arguments.size() == 1) {
                asm8086Parser.ArgumentContext argCtx = arguments.get(0);
                Register_Context regCtx = argCtx.register_();
                NameContext nameCtx = argCtx.name();
                NumberContext numberCtx = argCtx.number();
                if (regCtx != null) {
                    // El argumento es un solo registro
                    detectedReg1 = getRegistroFromStr(regCtx.REGISTRO().getText());
                    detectRegisterBits(detectedReg1);
                    return switch (detectedReg1) {
                        case AX, BX, CX, DX, DI, BP -> AddressingMode.REG16;
                        case AH, AL, BH, BL, CH, CL, DH, DL -> AddressingMode.REG8;
                        default -> AddressingMode.REG16;
                    };
                } else if (nameCtx != null) {
                    // El argumento es una etiqueta
                    // Verificar que existe en la tabla
                    Simbolo s = null;
                    if ((s = this.tablaSimbolos.getSimb(nameCtx.getText().toLowerCase())) == null) {
                        errMsg = String.format("no se encontró el simbolo %s", nameCtx.getText());
                        this.errorListener.syntaxError(null, null, linea, col, errMsg, null);
                    } else {
                        this.detectedMem1 = s.getDireccion();
                        detectedMMMBits = MMMField.MMM110;
                        detectedOOBits = OOModifierBits.OO00;
                    }
                    return AddressingMode.MEM;

                } else if (numberCtx != null) {
                    // El argumento es un número, por lo tanto es inmediato
                    return AddressingMode.INMEDIATO;
                } else if (argCtx.expression() != null) {
                    // Verificar si sale PTR
                    ExpressionContext actualExprCtx = argCtx.expression();
                    // if(argCtx.expression().argument(0).ptr() != null){
                    // Obtener el que sigue
                    // actualExprCtx =
                    // argCtx.expression().argument(0).expression().argument(0).expression();
                    // }
                    if (esMemoriaCompuesta(actualExprCtx)) {
                        return AddressingMode.MEM;
                    }
                }
            }
        } else if (expressions.size() == 2) {
            // Hay dos argumentos separados por comas
            ExpressionContext exprCtx1 = expressions.get(0);
            // List<MultiplyingExpressionContext> mpExpList1 =
            // exprCtx1.multiplyingExpression();
            // MultiplyingExpressionContext mpexpctx1 = mpExpList1.get(0);

            ExpressionContext exprCtx2 = expressions.get(1);
            // List<MultiplyingExpressionContext> mpExpList2 =
            // exprCtx2.multiplyingExpression();
            // MultiplyingExpressionContext mpexpctx2 = mpExpList2.get(0);

            asm8086Parser.ArgumentContext argCtx1 = exprCtx1.argument().get(0);
            asm8086Parser.ArgumentContext argCtx2 = exprCtx2.argument().get(0);

            // System.out.printf("Arg1: %s, Arg2: %s\n", argCtx1.getText(),
            // argCtx2.getText());

            if (argCtx1.register_() != null) {
                detectedReg1 = getRegistroFromStr(argCtx1.getText());
                if (argCtx2.register_() != null) {
                    detectedReg2 = getRegistroFromStr(argCtx2.getText());
                    return AddressingMode.REG_REG;
                } else if (argCtx2.number() != null) {
                    detectedInmStr = argCtx2.getText();
                    return AddressingMode.REG_INM;
                } else if (argCtx2.name() != null) {
                    Simbolo s = null;
                    if (((s = this.tablaSimbolos.getSimb(argCtx2.name().getText())) == null)) {
                        errMsg = String.format("no se encontró el simbolo %s", argCtx2.name().getText());
                        this.errorListener.syntaxError(null, null, linea, col, errMsg, null);
                    } else {
                        this.detectedMem2 = s.getDireccion();
                        this.detectedMMMBits = MMMField.MMM100;
                    }
                    return AddressingMode.REG_MEM;
                }else if(argCtx2.expression()!=null){
                    ExpressionContext actualExprCtx = argCtx2.expression();
                    if(esMemoriaCompuesta(argCtx2.expression())){
                        return AddressingMode.REG_MEM;
                    }
                }
            } else if (argCtx1.name() != null) {
                if (argCtx2.register_() != null) {
                    return AddressingMode.MEM_REG;
                } else if (argCtx2.number() != null) {
                    return AddressingMode.MEM_INM;
                }
            }else if(argCtx1.expression()!= null){
                if(esMemoriaCompuesta(argCtx1.expression())){
                    if(argCtx2.register_()!=null){
                        return AddressingMode.MEM_REG;
                    }else if(argCtx2.number()!=null){
                        detectedInmStr = argCtx2.getText();
                        return AddressingMode.REG_INM;
                    }
                }
            }
        }
        return AddressingMode.DESCONOCIDO;
    }

    public Registro getRegistroFromStr(String str) {
        return switch (str.toUpperCase()) {
            case "AX" -> Registro.AX;
            case "AH" -> Registro.AH;
            case "AL" -> Registro.AL;
            case "BX" -> Registro.BX;
            case "BH" -> Registro.BH;
            case "BL" -> Registro.BL;
            case "CX" -> Registro.CX;
            case "CH" -> Registro.CH;
            case "CL" -> Registro.CL;
            case "DX" -> Registro.DX;
            case "DH" -> Registro.DH;
            case "DL" -> Registro.DL;
            case "SI" -> Registro.SI;
            case "DI" -> Registro.DI;
            case "BP" -> Registro.BP;
            case "SP" -> Registro.SP;
            case "DS" -> Registro.DS;
            case "CS" -> Registro.CS;
            case "SS" -> Registro.SS;
            case "GS" -> Registro.GS;
            case "FS" -> Registro.FS;
            default -> Registro.UNKNOWN;
        };
    }

    public void detectRegisterBits(Registro reg){
        detectedOOBits = OOModifierBits.OO11;
        switch(reg){
            case AL, CL, DL, BL, AH,CH,BH,DH->{
                detectedW = TamañoSimbolo.BYTE;
            }
            case AX, CX, DX, BX, SP, BP, SI, DI->{
                detectedW = TamañoSimbolo.WORD;
            }
        }
        switch(reg){
            case AL, AX->{
                detectedMMMBits = MMMField.MMM000;
            }
            case CL, CX ->{
                detectedMMMBits = MMMField.MMM001;
            }
            case  DL, DX ->{
                detectedMMMBits = MMMField.MMM010;
            }
            case BL, BX ->{
                detectedMMMBits = MMMField.MMM011;
            }
            case AH,SP ->{
                detectedMMMBits = MMMField.MMM100;
            }
            case CH,BP ->{
                detectedMMMBits = MMMField.MMM101;
            }
            case DH, SI ->{
                detectedMMMBits = MMMField.MMM110;
            }
            case BH,DI ->{
                detectedMMMBits = MMMField.MMM111;
            }
        }
    }

    public boolean esMemoriaCompuesta(ExpressionContext exprCtx) {
        // Se asume que se entró al contexto entre corchetes []

        List<String> combinacionesValidas = new ArrayList<>();
        HashMap<String, MMMField> mmmBits = new HashMap<>();
        HashMap<String, OOModifierBits> ooBits = new HashMap<>();

        // Verificar que sigue alguno de estos formatos
        combinacionesValidas.add("BX + SI");
        mmmBits.put("BX + SI", MMMField.MMM000);
        ooBits.put("BX + SI", OOModifierBits.OO00);

        combinacionesValidas.add("BX + DI");
        mmmBits.put("BX + DI", MMMField.MMM001);
        ooBits.put("BX + DI", OOModifierBits.OO00);

        combinacionesValidas.add("BP + SI");
        mmmBits.put("BP + SI", MMMField.MMM010);
        ooBits.put("BP + SI", OOModifierBits.OO00);

        combinacionesValidas.add("BP + DI");
        mmmBits.put("BP + DI", MMMField.MMM011);
        ooBits.put("BP + DI", OOModifierBits.OO00);

        combinacionesValidas.add("SI");
        mmmBits.put("SI", MMMField.MMM100);
        ooBits.put("SI", OOModifierBits.OO00);

        combinacionesValidas.add("DI");
        mmmBits.put("DI", MMMField.MMM101);
        ooBits.put("DI", OOModifierBits.OO00);

        combinacionesValidas.add("BX");
        mmmBits.put("BX", MMMField.MMM111);
        ooBits.put("BX", OOModifierBits.OO00);

        combinacionesValidas.add("BX + SI + D16");
        mmmBits.put("BX + SI + D16", MMMField.MMM000);
        ooBits.put("BX + SI + D16", OOModifierBits.OO10);

        combinacionesValidas.add("BX + DI + D16");
        mmmBits.put("BX + DI + D16", MMMField.MMM001);
        ooBits.put("BX + DI + D16", OOModifierBits.OO10);

        combinacionesValidas.add("BP + SI + D16");
        mmmBits.put("BP + SI + D16", MMMField.MMM010);
        ooBits.put("BP + SI + D16", OOModifierBits.OO01);

        combinacionesValidas.add("BP + DI + D16");
        mmmBits.put("BP + DI + D16", MMMField.MMM011);
        ooBits.put("BP + DI + D16", OOModifierBits.OO10);

        combinacionesValidas.add("SI + D16");
        mmmBits.put("SI + D16", MMMField.MMM100);
        ooBits.put("SI + D16", OOModifierBits.OO10);

        combinacionesValidas.add("DI + D16");
        mmmBits.put("DI + D16", MMMField.MMM101);
        ooBits.put("DI + D16", OOModifierBits.OO10);

        combinacionesValidas.add("BP + D16");
        mmmBits.put("BP + D16", MMMField.MMM110);
        ooBits.put("BP + D16", OOModifierBits.OO10);

        combinacionesValidas.add("BX + D16");
        mmmBits.put("BX + D16", MMMField.MMM111);
        ooBits.put("BX + D16", OOModifierBits.OO10);

        combinacionesValidas.add("BX + SI + D8");
        mmmBits.put("BX + SI + D8", MMMField.MMM000);
        ooBits.put("BX + SI + D8", OOModifierBits.OO01);

        combinacionesValidas.add("BX + DI + D8");
        mmmBits.put("BX + DI + D8", MMMField.MMM001);
        ooBits.put("BX + DI + D8", OOModifierBits.OO01);

        combinacionesValidas.add("BP + SI + D8");
        mmmBits.put("BP + SI + D8", MMMField.MMM010);
        ooBits.put("BP + SI + D8", OOModifierBits.OO01);

        combinacionesValidas.add("BP + DI + D8");
        mmmBits.put("BP + DI + D8", MMMField.MMM011);
        ooBits.put("BP + DI + D8", OOModifierBits.OO01);

        combinacionesValidas.add("SI + D8");
        mmmBits.put("SI + D8", MMMField.MMM100);
        ooBits.put("SI + D8", OOModifierBits.OO01);

        combinacionesValidas.add("DI + D8");
        mmmBits.put("DI + D8", MMMField.MMM101);
        ooBits.put("DI + D8", OOModifierBits.OO01);

        combinacionesValidas.add("BP + D8");
        mmmBits.put("BP + D8", MMMField.MMM110);
        ooBits.put("BP + D8", OOModifierBits.OO01);

        combinacionesValidas.add("BX + D8");
        mmmBits.put("BX + D8", MMMField.MMM111);
        ooBits.put("BX + D8", OOModifierBits.OO01);

        List<Token> flattenedTokens = new ArrayList<>();
        List<String> flattenedSymbols = new ArrayList<>();

        if (exprCtx.signo(0) != null) {
            flattenedTokens.add(exprCtx.argument(0).getStart());
            flattenedTokens.add(exprCtx.signo(0).getStart());
            flattenedTokens.add(exprCtx.argument(1).getStart());
            if (exprCtx.signo(1) != null) {
                flattenedTokens.add(exprCtx.signo(0).getStart());
                if (exprCtx.argument(2) != null) {
                    flattenedTokens.add(exprCtx.argument(2).getStart());
                }
            }
        } else {
            flattenedTokens.add(exprCtx.argument(0).getStart());
        }

        flattenedSymbols = flattenedTokens.stream().map(t -> {
            if (vocab.getSymbolicName(t.getType()) != null && vocab.getSymbolicName(t.getType()).equals("NUM")) {
                if (!testOverflow(t.getText(), TamañoSimbolo.BYTE)) {
                    detectedDisp8 =  (byte)(parseNumberLiteral(t.getText()) & 0xFF);
                    return "D8";
                } else {
                    detectedDisp16 = codificaDisp16(parseNumberLiteral(t.getText()));
                    return "D16";
                }
            } else {
                return t.getText();
            }
        }).toList();

        String combinacionObtenida = String.join(" ", flattenedSymbols);
        // System.out.println("unido:" + combinacionObtenida);

        for (String comb : combinacionesValidas) {
            if (combinacionObtenida.equals(comb)) {
                detectedMMMBits = mmmBits.get(combinacionObtenida);
                detectedOOBits = ooBits.get(combinacionObtenida);
                return true;
            }
        }

        return false;
    }

    byte[] codificaDisp16(int d){
        byte[] disp = Ints.toByteArray(d);
        return Arrays.copyOfRange(disp, 2,4);
    }
}
