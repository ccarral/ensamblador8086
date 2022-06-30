package com.ensambladores.sym;

import java.util.ArrayDeque;

public class TablaSimbolos {
    private ArrayDeque<Simbolo> tablaInner;
    private long contador = 0x0;

    public TablaSimbolos(long offset) {
        this.tablaInner = new ArrayDeque<>();
        this.contador = offset;
    }

    public TablaSimbolos() {
        this.tablaInner = new ArrayDeque<>();
        this.contador = 0;
    }

    public ArrayDeque<Simbolo> getTablaInner() {
        return this.tablaInner;
    }

    public long incrementaContador(int o) {
        return this.contador++;
    }

    public void añadeSimbolo(String etiqueta, int dir) {
        Simbolo simb = new Simbolo();
        simb.setEtiqueta(etiqueta.toLowerCase());
        simb.setDireccion(dir);
        simb.setTipo(TipoSimbolo.WORD);
        tablaInner.add(simb);
    }

    public void setTipoUltimoAñadido(TipoSimbolo tipoSimbolo) {
        Simbolo ultimoAñadido = this.tablaInner.pollLast();
        if (ultimoAñadido != null) {
            ultimoAñadido.setTipo(tipoSimbolo);
            this.tablaInner.addLast(ultimoAñadido);
        }
    }

    public Simbolo getSimb(String etiqueta) {
        // return this.tablaInner.get(etiqueta.toLowerCase());
        return null;
    }

    public String toString() {
        String out = null;
        out = String.format("-------------------------\n");
        out = out.concat(String.format("%10s %4s %10s\n", "simb", "tipo", "dir"));
        out = out.concat("-------------------------\n");
        for (Simbolo s : this.getTablaInner()) {
            out = out.concat(String.format("%10s %4s 0x%08X\n", s.getEtiqueta(), s.getTipo(), s.getDireccion()));
        }
        out = out.concat("-------------------------\n");
        return out;
    }
}
