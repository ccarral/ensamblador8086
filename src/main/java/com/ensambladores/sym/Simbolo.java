package com.ensambladores.sym;

public class Simbolo {
    private TamañoSimbolo tam = TamañoSimbolo.WORD;

    public int getLongitud() {
        return longitud;
    }

    public void setLongitud(int longitud) {
        this.longitud = longitud;
    }

    private int longitud = 4;


    private TipoSimbolo tipoSimbolo = TipoSimbolo.ETQ;
    private String etiqueta;
    private int direccion;

    public int getDireccion() {
        return direccion;
    }

    public void setDireccion(int direccion) {
        this.direccion = direccion;
    }

    public Simbolo(String etq) {
        this.etiqueta = etq;
    }

    public TipoSimbolo getTipoSimbolo() {
        return tipoSimbolo;
    }

    public void setTipoSimbolo(TipoSimbolo tipoSimbolo) {
        this.tipoSimbolo = tipoSimbolo;
    }
    public TamañoSimbolo getTam() {
        return tam;
    }

    public void setTam(TamañoSimbolo tam) {
        this.tam = tam;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public void setEtiqueta(String etiqueta) {
        this.etiqueta = etiqueta;
    }


    public Simbolo(TamañoSimbolo tam, String etiqueta) {
        this.etiqueta = etiqueta;
        this.tam = tam;
    }
}
