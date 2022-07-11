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

    public void añadeSimbolo(Simbolo s) {
        tablaInner.add(s);
    }

    public void setTamUltimoAñadido(TamañoSimbolo tamañoSimbolo) {
        Simbolo ultimoAñadido = this.tablaInner.pollLast();
        if (ultimoAñadido != null) {
            ultimoAñadido.setTam(tamañoSimbolo);
            this.tablaInner.addLast(ultimoAñadido);
        }
    }

    public void setTipoUltimoAñadido(TipoSimbolo tipoSimbolo) {
        Simbolo ultimoAñadido = this.tablaInner.pollLast();
        if (ultimoAñadido != null) {
            ultimoAñadido.setTipoSimbolo(tipoSimbolo);
            this.tablaInner.addLast(ultimoAñadido);
            System.out.println("Cambiando "+ ultimoAñadido.getEtiqueta());
        }
    }




    public Simbolo getSimb(String etiqueta) {
        for(Simbolo s: this.tablaInner){
            if(s.getEtiqueta().equals(etiqueta.toLowerCase())){
                return s;
            }
        }
        return null;
    }

    public String toString() {
        String out = null;
        out = String.format("---------------------------------\n");
        out = out.concat(String.format("%10s %5s %5s %-5s %6s\n", "simb","tipo","B/W", "dir", "#bytes"));
        out = out.concat("---------------------------------\n");
        for (Simbolo s : this.getTablaInner()) {
            out = out.concat(String.format("%10s %5s %5s %04X %5d\n", s.getEtiqueta(), s.getTipoSimbolo(), s.getTam(), s.getDireccion(), s.getLongitud()));
        }
        out = out.concat("---------------------------------\n");
        return out;
    }
}
