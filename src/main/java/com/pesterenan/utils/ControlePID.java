package com.pesterenan.utils;

import static com.pesterenan.utils.Utilities.clamp;
import static java.lang.System.currentTimeMillis;

/**
 * Controlador Proporcional Integral Derivativo
 * <p>
 * Data: 22/08/2018
 * Atualizado: 06/06/2022
 *
 * @author : Renan Torres <pesterenan@gmail.com>
 * @version 1.2
 * @since 1.0
 */

public class ControlePID {

    private double limiteMin = -1;
    private double limiteMax = 1;
    private double kp = 0.025;
    private double ki = 0.05;
    private double kd = 0.01;
    private double amostragem = 25;
    private double valorSaida = 1;
    private double termoIntegral = 0;
    private double ultimaEntrada = 0;
    private double ultimoCalculo = 0;

    private double limitarValor(double valor) {
        return clamp(valor, this.limiteMin, this.limiteMax);
    }

    /**
     * Computar o valor de saida do controlador, usando os valores de entrada e
     * limite
     *
     * @returns Valor computado do controlador
     */
    public double computarPID(double valorEntrada, double valorLimite) {
        double agora = currentTimeMillis();
        double mudancaTempo = agora - this.ultimoCalculo;

        if (mudancaTempo >= this.amostragem) {
            double erro = valorLimite - valorEntrada;
            double diferencaEntrada = (valorEntrada - this.ultimaEntrada);

            this.termoIntegral = limitarValor(this.termoIntegral + ki * erro);
            this.valorSaida = limitarValor(kp * erro + ki * this.termoIntegral - kd * diferencaEntrada);

            this.ultimaEntrada = valorEntrada;
            this.ultimoCalculo = agora;
        }

        return valorSaida;
    }

    public void limitarSaida(double min, double max) {
        if (min < max) {
            this.limiteMin = min;
            this.limiteMax = max;
            this.termoIntegral = limitarValor(this.termoIntegral);
        }
    }

    public void ajustarPID(double Kp, double Ki, double Kd) {
        if (Kp > 0) {
            this.kp = Kp;
        }
        if (Ki >= 0) {
            this.ki = Ki;
        }
        if (Kd >= 0) {
            this.kd = Kd;
        }
    }

    public void setAmostragem(double milissegundos) {
        if (milissegundos > 0) {
            this.amostragem = milissegundos;
        }
    }

}
