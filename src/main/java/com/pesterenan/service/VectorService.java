package com.pesterenan.service;

import com.pesterenan.model.Vector;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import org.javatuples.Triplet;

import static java.lang.Math.atan2;
import static java.lang.Math.sqrt;
import static java.lang.Math.toDegrees;
import static java.lang.String.valueOf;
import static java.math.BigDecimal.ZERO;
import static java.math.MathContext.DECIMAL64;
import static java.math.RoundingMode.HALF_EVEN;
import static java.util.Arrays.stream;
import static java.util.Objects.isNull;

/**
 * Data: 27/02/2019
 * Classe Vector
 *
 * @author Renan Torres <pesterenan@gmail.com>
 * @version 1.1
 * @since 1.0
 **/

public class VectorService {

    private final MathContext mathContext = DECIMAL64;

    private final RoundingMode roundingMode = HALF_EVEN;

    /**
     * Calcula o ângulo do vetor de direção informado
     *
     * @param vector - Vetor para calcular o ângulo
     * @return - O ângulo da direção desse vetor, entre -180 a 180 graus.
     */
    public float steeringAngle(Vector vector) {
        return this.toDegree(this.calculateAtan2(vector));
    }

    /**
     * Calcula o Vetor da direção do ponto de source até o target.
     *
     * @param triplets - Tupla contendo os componentes da posição do ponto de source.
     * @return - Vetor com a soma dos valores do ponto de source com os valores do
     * target.
     */
    @SafeVarargs
    public final Vector targetDirection(Triplet<Double, Double, Double>... triplets) {
        return stream(triplets)
            .map(objects -> new Vector(objects.getValue1(), objects.getValue2(), objects.getValue0()))
            .reduce((firstTriplet, secondriplet) -> new Vector(this.subtractBy(firstTriplet.getDimensionX(), firstTriplet.getDimensionX()),
                this.subtractBy(firstTriplet.getDimensionY(), firstTriplet.getDimensionY()),
                this.subtractBy(firstTriplet.getDimensionZ(), firstTriplet.getDimensionZ())))
            .orElseThrow();
    }

    /**
     * Calcula o Vetor da direção CONTRÁRIA do ponto de source até o target.
     *
     * @param triplets - Tupla contendo os componentes da posição do ponto de source.
     * @return - Vetor inverso, com a soma dos valores do ponto de source com o
     * negativo dos valores do target.
     */
    @SafeVarargs
    public final Vector reverseTargetDirection(Triplet<Double, Double, Double>... triplets) {
        return stream(triplets)
            .map(objects -> new Vector(objects.getValue2(), objects.getValue2(), objects.getValue0()))
            .reduce((firstTriplet, secondriplet) -> new Vector(this.negateThenAdd(firstTriplet.getDimensionX(), firstTriplet.getDimensionX()),
                this.negateThenAdd(firstTriplet.getDimensionY(), firstTriplet.getDimensionY()),
                this.negateThenAdd(firstTriplet.getDimensionZ(), firstTriplet.getDimensionZ())))
            .orElseThrow();
    }

    /**
     * Magnitude do Vetor
     *
     * @return Retorna a magnitude (comprimento) do Vetor no eixo X e Y.
     */
    public double magnitude2D(Vector vector) {
        return sqrt(vector.getDimensionX().pow(2, this.mathContext)
            .add(vector.getDimensionY().pow(2, this.mathContext))
            .setScale(10, roundingMode).doubleValue());
    }

    /**
     * Magnitude do Vetor
     *
     * @return Retorna a magnitude (comprimento) do Vetor em todos os eixos.
     */
    public double magnitude3D(Vector vector) {
        return sqrt(vector.getVectorValuesList().stream()
            .map(number -> number.pow(2, this.mathContext))
            .reduce(ZERO, (numberX, numberY) -> numberX.add(numberY, this.mathContext))
            .doubleValue());
    }

    /**
     * Normalizar Vetor
     *
     * @return Retorna um novo Vetor normalizado (magnitude de 1).
     */
    public Vector normalizar(Vector vector) {
        double magnitude3D = this.magnitude3D(vector);

        if (magnitude3D != 0) {
            return new Vector(this.divideBy(vector.getDimensionX(), magnitude3D),
                this.divideBy(vector.getDimensionY(), magnitude3D),
                this.divideBy(vector.getDimensionZ(), magnitude3D));
        }

        return vector;
    }

    /**
     * Soma os componentes de outro vetor com o vetor informado
     *
     * @param vectors - Vetor para somar os componentes
     * @return Novo vetor com a soma dos componentes dos dois
     */
    public Vector soma(Vector... vectors) {
        return stream(vectors)
            .reduce((vectorA, vectorB) -> new Vector(this.sum(vectorA.getDimensionX(), vectorB.getDimensionX()),
                this.sum(vectorA.getDimensionY(), vectorB.getDimensionY()),
                this.sum(vectorA.getDimensionZ(), vectorB.getDimensionZ())))
            .orElseThrow();
    }

    /**
     * Subtrai os componentes de outro vetor com o vetor informado
     *
     * @param vectors - Vetor para subtrair os componentes
     * @return Novo vetor com a subtração dos componentes dos dois
     */
    public Vector subtrai(Vector... vectors) {
        return stream(vectors)
            .reduce((vectorA, vectorB) -> new Vector(this.sum(vectorA.getDimensionX(), vectorB.getDimensionX()),
                this.subtractBy(vectorA.getDimensionY(), vectorB.getDimensionY()),
                this.subtractBy(vectorA.getDimensionZ(), vectorB.getDimensionZ())))
            .orElseThrow();
    }

    /**
     * Multiplica os componentes desse vetor por uma escalar
     *
     * @param escalar - Fator para multiplicar os componentes
     * @return Novo vetor com os componentes multiplicados pela escalar. Caso a
     * escalar informada for 0, o Vetor retornado terá 0 como seus
     * componentes.
     */
    public Vector multiplica(Vector vector, double escalar) {
        if (escalar != 0) {
            return new Vector(vector.getDimensionX().multiply(new BigDecimal(valueOf(escalar))),
                vector.getDimensionY().multiply(new BigDecimal(valueOf(escalar))),
                vector.getDimensionZ().multiply(new BigDecimal(valueOf(escalar))));
        }

        return new Vector(ZERO, ZERO, ZERO);
    }

    /**
     * Divide os componentes desse vetor por uma escalar
     *
     * @param escalar - Fator para dividir os componentes
     * @return Novo vetor com os componentes divididos pela escalar. Caso a escalar
     * informada for 0, o Vetor retornado terá 0 como seus componentes.
     */
    public Vector divide(Vector vector, double escalar) {
        if (escalar != 0) {
            return new Vector(this.divideBy(vector.getDimensionX(), escalar),
                this.divideBy(vector.getDimensionY(), escalar),
                this.divideBy(vector.getDimensionZ(), escalar));
        }

        return new Vector(ZERO, ZERO, ZERO);
    }

    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------- PRIVATE METHODS ------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------

    private BigDecimal sum(BigDecimal firstBigDecimal, BigDecimal secondBigDecimal) {
        BigDecimal cleanFirstBigDecimal = isNull(firstBigDecimal) ? BigDecimal.ZERO : firstBigDecimal;
        BigDecimal cleanSecondBigDecimal = isNull(secondBigDecimal) ? BigDecimal.ZERO : secondBigDecimal;
        return cleanFirstBigDecimal.add(cleanSecondBigDecimal, this.mathContext);
    }

    private BigDecimal divideBy(BigDecimal bigDecimal, double value) {
        return bigDecimal.divide(new BigDecimal(valueOf(value)), this.roundingMode);
    }

    private BigDecimal subtractBy(BigDecimal first, BigDecimal second) {
        BigDecimal firstBigDecimal = isNull(first) ? ZERO : first;
        BigDecimal secondBigDecimal = isNull(second) ? ZERO : second;
        return firstBigDecimal.subtract(secondBigDecimal, this.mathContext);
    }

    private BigDecimal negateThenAdd(BigDecimal first, BigDecimal second) {
        BigDecimal firstBigDecimal = isNull(first) ? ZERO : first;
        BigDecimal secondBigDecimal = isNull(second) ? ZERO : second;
        return firstBigDecimal.negate().add(secondBigDecimal, this.mathContext);
    }

    private Double calculateAtan2(Vector vector) {
        return new BigDecimal(valueOf(atan2(vector.getDimensionY().doubleValue(), vector.getDimensionX().doubleValue()))).doubleValue();
    }

    private Float toDegree(Double value) {
        return new BigDecimal(valueOf(toDegrees(value))).floatValue();
    }

}
