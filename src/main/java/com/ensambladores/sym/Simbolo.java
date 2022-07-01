package com.ensambladores.sym;

public class Simbolo {
    private TipoSimbolo tipo;
    private String etiqueta;
    private int direccion;

    public int getDireccion() {
        return direccion;
    }

    public void setDireccion(int direccion) {
        this.direccion = direccion;
    }

    public Simbolo() {
    }

    public TipoSimbolo getTipo() {
        return tipo;
    }

    public void setTipo(TipoSimbolo tipo) {
        this.tipo = tipo;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public void setEtiqueta(String etiqueta) {
        this.etiqueta = etiqueta;
    }


    public Simbolo(TipoSimbolo tipo, String etiqueta) {
        this.etiqueta = etiqueta;
        this.tipo = tipo;
    }
}
