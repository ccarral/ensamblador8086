package com.ensambladores.instrucciones;

import com.ensambladores.sym.TamañoSimbolo;

public enum Registro {
    AL,
    AH,
    AX,
    BL,
    BH,
    BX,
    CL,
    CH,
    CX,
    DL,
    DH,
    DX,
    SP,
    BP,
    SI,
    DI,
    ES,
    CS,
    SS,
    DS,
    FS,
    GS,
    UNKNOWN;

    public RRRField getRRRBits(){
        return switch(this){
            case AX, AL, ES -> RRRField.RRR000;
            case CX, CL, CS -> RRRField.RRR001;
            case DX, DL, SS -> RRRField.RRR010;
            case BX, BL, DS -> RRRField.RRR011;
            case SP,AH, FS -> RRRField.RRR100;
            case BP,CH, GS -> RRRField.RRR101;
            case SI,DH -> RRRField.RRR110;
            case DI, BH, UNKNOWN -> RRRField.RRR111;
        };
    }

    public TamañoSimbolo getW(){
        return switch(this){
            case AX, BX, CX, DX, SP,BP,SI,DI -> TamañoSimbolo.WORD;
            case AL,CL,DL,BL,AH,CH,DH,BH -> TamañoSimbolo.BYTE;
            default -> TamañoSimbolo.WORD;
        };
    }

}
