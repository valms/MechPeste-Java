package com.pesterenan.model;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.javatuples.Triplet;

import static java.lang.String.valueOf;
import static java.util.Arrays.asList;

@Getter
@Setter
@AllArgsConstructor
public class Vector {

    private BigDecimal dimensionX;

    private BigDecimal dimensionY;

    private BigDecimal dimensionZ;

    private final List<BigDecimal> vectorValuesList;

    /**
     * Cria um vetor informando valores X,Y,Z manualmente
     *
     * @param dimensionX - Valor eixo X
     * @param dimensionY - Valor eixo Y
     * @param dimensionZ - Valor eixo Z
     */
    public Vector(Double dimensionX, Double dimensionY, Double dimensionZ) {
        this.dimensionX = new BigDecimal(valueOf(dimensionX));
        this.dimensionY = new BigDecimal(valueOf(dimensionY));
        this.dimensionZ = new BigDecimal(valueOf(dimensionZ));

        this.vectorValuesList = asList(this.dimensionX, this.dimensionY, this.dimensionZ);
    }

    public Vector(BigDecimal dimensionX, BigDecimal dimensionY, BigDecimal dimensionZ) {
        this.dimensionX = dimensionX;
        this.dimensionY = dimensionY;
        this.dimensionZ = dimensionZ;

        this.vectorValuesList = asList(this.dimensionX, this.dimensionY, this.dimensionZ);
    }

    public Vector(Float dimensionX, Double dimensionY, Float dimensionZ) {
        this.dimensionX = new BigDecimal(valueOf(dimensionX));
        this.dimensionY = new BigDecimal(valueOf(dimensionY));
        this.dimensionZ = new BigDecimal(valueOf(dimensionZ));

        this.vectorValuesList = asList(this.dimensionX, this.dimensionY, this.dimensionZ);
    }

    /**
     * Cria um vetor com valores de uma tupla (Triplet)
     *
     * @param tupla - Triplet com valores X,Y,Z em conjunto
     */
    public Vector(Triplet<Double, Double, Double> tupla) {
        this.dimensionX = new BigDecimal(valueOf(tupla.getValue0()));
        this.dimensionY = new BigDecimal(valueOf(tupla.getValue1()));
        this.dimensionZ = new BigDecimal(valueOf(tupla.getValue2()));

        this.vectorValuesList = asList(this.dimensionX, this.dimensionY, this.dimensionZ);
    }


    /**
     * Transforma um Vetor em uma tupla com os valores.
     * <p>
     * Vetor para transformar em tupla.
     *
     * @return - Nova tupla contendo os valores do vetor em seus componentes.
     */
    public Triplet<Double, Double, Double> toTriplet() {
        return new Triplet<>(this.dimensionX.doubleValue(), this.dimensionY.doubleValue(), this.dimensionZ.doubleValue());
    }


    /**
     * Retorna um String com os valores do Vetor
     *
     * @return ex: "(X: 3.0, Y: 4.0, Z: 5.0)"
     */
    @Override
    public String toString() {
        return String.format("( X: %.2f Y: %.2f Z: %.2f)", dimensionX, dimensionY, dimensionZ);
    }

}
