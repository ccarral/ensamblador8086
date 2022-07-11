package com.ensambladores.instrucciones;

import java.util.EnumSet;

public enum OpCode {
    PUSHF,
    DAA,
    STOSB,
    LODSB,
    CBW,
    POPF,
    DEC,
    POP,
    NEG,
    IDIV,
    XOR,
    LES,
    SHL,
    MOV,
    LOOPNE,
    JNAE,
    JP,
    JC,
    JNL,
    JAE,
    UNKNOWN;

    public EnumSet<AddressingMode> getAddressingModes() {
        return switch (this) {
            case PUSHF, DAA, STOSB, LODSB, CBW, POPF -> EnumSet.of(AddressingMode.IMPL);
            case DEC -> EnumSet.of(AddressingMode.REG8, AddressingMode.MEM, AddressingMode.REG16);
            case POP -> EnumSet.of(AddressingMode.MEM, AddressingMode.REG16, AddressingMode.SEG);
            case NEG -> EnumSet.of(AddressingMode.MEM);
            case IDIV -> EnumSet.of(AddressingMode.MEM);
            case XOR -> EnumSet.of(AddressingMode.REG_REG, AddressingMode.MEM_REG, AddressingMode.REG_MEM, AddressingMode.INMEDIATO, AddressingMode.REG_INM);
            case LES -> EnumSet.of(AddressingMode.REG_MEM);
            case SHL -> EnumSet.of(AddressingMode.REG_INM);
            // Faltan las de acumulador
            case MOV -> EnumSet.of(AddressingMode.REG_REG, AddressingMode.MEM_REG, AddressingMode.REG_MEM, AddressingMode.MEM_INM);
            case LOOPNE , JNAE, JP, JC, JNL, JAE-> EnumSet.of(AddressingMode.MEM);
            default -> EnumSet.of(AddressingMode.DESCONOCIDO);
        };
    }
}
