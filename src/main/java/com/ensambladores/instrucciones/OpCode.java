package com.ensambladores.instrucciones;

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

    public int getProps() {
        switch (this) {
            case PUSHF:
                return OpCodeProperties.NO_ARGS;
            case DAA:
                return OpCodeProperties.NO_ARGS;
            default:
                return 0;
        }
    }
}
