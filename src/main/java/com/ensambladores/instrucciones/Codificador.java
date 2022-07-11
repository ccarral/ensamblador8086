package com.ensambladores.instrucciones;

import java.util.Arrays;

import com.google.common.primitives.Ints;

public class Codificador {
    public static byte[] codifica(OpCode opCode) {
        switch (opCode) {
            case PUSHF:
                return new byte[]{0b0011100};
            case DAA:
                return new byte[]{0b00100111};
            case STOSB:
                return new byte[]{(byte) 0b10101010};
            case LODSB:
                return new byte[]{(byte) 0b10101100};
            case CBW:
                return new byte[]{(byte) 0b10011000};
            case POPF:
                return new byte[]{(byte) 0b10010000};
            case DEC:
                return new byte[]{};
            case IDIV:
                return new byte[]{};
            case JAE:
                return new byte[]{0b01110110};
            case JC:
                return new byte[]{};
            case JNAE:
                return new byte[]{0b01110010};
            case JNL:
                return new byte[]{0b01111101};
            case JP:
                return new byte[]{};
            case LES:
                return new byte[]{};
            case LOOPNE:
                return new byte[]{(byte) 0b11100000};
            case MOV:
                return new byte[]{};
            case NEG:
                return new byte[]{};
            case POP:
                return new byte[]{};
            case SHL:
                return new byte[]{};
            case UNKNOWN:
                return new byte[]{};
            case XOR:
                return new byte[]{};
            default:
                return new byte[]{};
        }
    }

    public static boolean noSoportado(OpCode opCode) {
        return Codificador.codifica(opCode).length == 0;
    }

    public static boolean esSaltoLargo(int dir, int dest) {
        if (dest > dir) {
            return dest - dir > 127;
        } else {
            return dir - dest > 128;
        }
    }

    public static byte[] codificaSalto(int dir, int dest) {
        if (esSaltoLargo(dir, dest)) {
            // Regresa la repr. completa del destino como 2 bytes
            byte[] destBytes = Ints.toByteArray(dest);
            return Arrays.copyOfRange(destBytes, 2, 4);
        } else {
            int desp = dest - dir;
            if (desp < 0) {
                desp |= (byte) (1 << 8);
            }
            //System.out.printf("dir:%s, dest:%s\n", dir, dest);
            //System.out.println("desp :" + desp);
            // Regresa 1 byte
            byte[] despBytes = Ints.toByteArray(desp);
            //System.out.println(Arrays.toString(despBytes));
            return Arrays.copyOfRange(despBytes, 3, 4);

        }

    }

    public static InstructionTemplate getTemplate(OpCode opc, AddressingMode am, boolean longJump) {
        InstructionTemplate default_ = new InstructionTemplate("11111111");
        InstructionTemplate template =
                switch (opc) {
                    case MOV, NEG, IDIV, LES, SHL, UNKNOWN -> default_;
                    case PUSHF -> new InstructionTemplate("10011100");
                    case STOSB -> new InstructionTemplate("10101010");
                    case DAA -> new InstructionTemplate("00100111");
                    case LODSB -> new InstructionTemplate("10101100");
                    case CBW -> new InstructionTemplate("10011000");
                    case POPF -> new InstructionTemplate("10011101");
                    case DEC -> switch (am) {
                        case REG8, MEM -> new InstructionTemplate("1111111woo001mmm");
                        case REG16 -> new InstructionTemplate("01001rrr");
                        default -> default_;
                    };
                    case POP -> switch (am) {
                        case MEM -> new InstructionTemplate("10001111oo000mmmdisp");
                        default -> default_;
                    };
                    case XOR -> switch(am){
                        case REG_MEM -> new InstructionTemplate("0011001woorrrmmmdisp");
                        default -> default_;
                    };
                    case LOOPNE -> new InstructionTemplate("11100000disp");
                    case JNAE -> {
                        if (longJump) {
                            yield new InstructionTemplate("0000111110000010disp");
                        } else {
                            yield new InstructionTemplate("01110010disp");
                        }
                    }
                    case JP -> {
                        if (longJump) {
                            yield new InstructionTemplate("0000111110001010disp");
                        } else {
                            yield new InstructionTemplate("01110010disp");
                        }
                    }
                    case JC -> {
                        if (longJump) {
                            yield new InstructionTemplate("0000111110000010disp");
                        } else {
                            yield new InstructionTemplate("01110010disp");
                        }
                    }
                    case JNL -> {
                        if(longJump) {
                            yield new InstructionTemplate("0000111110001101disp");
                        }else {
                            yield new InstructionTemplate("01111101disp");
                        }
                    }
                    case JAE -> {
                        if(longJump) {
                            yield new InstructionTemplate("0000111110000010disp");
                        }else {
                            yield new InstructionTemplate("01110011disp");
                        }
                    }
                };

        return template;

    }
}
