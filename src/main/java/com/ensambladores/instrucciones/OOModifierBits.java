package com.ensambladores.instrucciones;

public enum OOModifierBits {
    OO00,
    OO01,
    OO10,
    OO11,;

    @Override
    public String toString() {
        return super.toString().substring(2,4);
    }

}
