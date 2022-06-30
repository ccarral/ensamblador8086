package com.ensambladores.instrucciones;

public class Codificador {
    public static String codifica(OpCode opCode){
        switch(opCode){
            case PUSHF:
                return "10011100";
            case DAA:
                return "00100111";
            default:
                return "";
        }
    }    
}
