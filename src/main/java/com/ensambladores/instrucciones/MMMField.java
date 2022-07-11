package com.ensambladores.instrucciones;

public enum MMMField {
    MMM000,
    MMM001,
    MMM010,
    MMM011,
    MMM100,
    MMM101,
    MMM110,
    MMM111,;

    @Override
    public String toString() {
        return super.toString().substring(3, 6);
    }
    
}
