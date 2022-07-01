package com.pesterenan.enums;

public enum Modulos {

    APOASTRO("Apoastro"),
    PERIASTRO("Periastro"),
    EXECUTAR("Executar"),
    AJUSTAR("Ajustar"),
    DIRECAO("Direção"),
    MODULO("Módulo"),
    FUNCAO("Função"),
    MODULO_DECOLAGEM("Executar Decolagem"),
    MODULO_MANOBRAS("Módulo Manobras"),
    MODULO_POUSO("Módulo Pouso"),
    MODULO_PILOTO("Módulo Piloto"),
    ALTITUDE_SOBREVOO("Altitude Sobrevoo"),
    MODULO_POUSO_SOBREVOAR("Sobrevoar"),
    INCLINACAO("Inclinação"),
    CIRCULAR("Circular"),
    QUADRATICA("Quadrática"),
    CUBICA("Cúbica"),
    SINUSOIDAL("Sinusoidal"),
    EXPONENCIAL("Exponencial"),
    ROLAGEM("Rolagem");

    final String description;

    Modulos(String description) {
        this.description = description;
    }

    public String get() {
        return this.description;
    }

}
