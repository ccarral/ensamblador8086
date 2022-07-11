package com.ensambladores.instrucciones;

public enum RRRField {
    RRR000,
    RRR001,
    RRR010,
    RRR011,
    RRR100,
    RRR101,
    RRR110,
    RRR111,;

    @Override
    public String toString() {
        return super.toString().substring(3,6);
    }
}
