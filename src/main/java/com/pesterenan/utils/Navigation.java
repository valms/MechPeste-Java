package com.pesterenan.utils;

import com.pesterenan.model.SpaceShip;
import com.pesterenan.model.Vector;
import com.pesterenan.service.VectorService;
import java.io.IOException;
import krpc.client.RPCException;
import krpc.client.StreamException;
import org.javatuples.Triplet;

import static com.pesterenan.utils.Utilities.clamp;
import static com.pesterenan.utils.Utilities.remap;

public class Navigation {

    private final SpaceShip spaceShip;

    private final VectorService vectorService;

    public Navigation(SpaceShip spaceShip) {
        this.spaceShip = spaceShip;
        this.vectorService = new VectorService();
    }

    public void mirarRetrogrado() throws RPCException {
        this.mirarNaDirecao(this.spaceShip.getFlightParameters().getRetrograde());
    }

    public void mirarRadialDeFora() throws RPCException {
        this.mirarNaDirecao(this.spaceShip.getFlightParameters().getRadial());
    }

    public void mirarNaDirecao(Triplet<Double, Double, Double> direction) {
        try {
            Triplet<Double, Double, Double> posicaoAlvo = this.spaceShip.getSpaceCenter().transformPosition(direction,
                this.spaceShip.getActiveVessel().getSurfaceVelocityReferenceFrame(), this.spaceShip.getPontoRefOrbital());

            Vector vetorDirecaoHorizontal = this.vectorService.reverseTargetDirection(this.spaceShip.getActiveVessel().position(this.spaceShip.getPontoRefSuperficie()),
                this.spaceShip.getSpaceCenter().transformPosition(posicaoAlvo, this.spaceShip.getPontoRefOrbital(), this.spaceShip.getPontoRefSuperficie()));

            Vector alinharDirecao = this.getElevacaoDirecaoDoVetor(vetorDirecaoHorizontal);

            this.spaceShip.getActiveVessel().getAutoPilot()
                .targetPitchAndHeading(alinharDirecao.getDimensionY().floatValue(), alinharDirecao.getDimensionX().floatValue());
            this.spaceShip.getActiveVessel().getAutoPilot().setTargetRoll(90);

        } catch (RPCException | StreamException | IOException e) {
            System.err.println("Não foi possível manobrar a nave.");
        }
    }

    private Vector getElevacaoDirecaoDoVetor(Vector alvo) throws RPCException, IOException, StreamException {
        Triplet<Double, Double, Double> speedTriplet = this.getSpeedTriplet();
        Vector speed = new Vector(speedTriplet.getValue1(), speedTriplet.getValue2(), speedTriplet.getValue0());
        alvo = this.vectorService.subtrai(alvo, speed);
        return new Vector(this.vectorService.steeringAngle(alvo), this.getClamp(), this.vectorService.steeringAngle(speed));
    }

    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------- PRIVATE METHODS ------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------

    private Triplet<Double, Double, Double> getSpeedTriplet() throws RPCException {
        return this.spaceShip.getSpaceCenter().transformPosition(this.spaceShip.getFlightParameters().getVelocity(),
            this.spaceShip.getPontoRefOrbital(), this.spaceShip.getPontoRefSuperficie());
    }

    private double getClamp() throws RPCException, StreamException {
        return clamp(this.getRemap(), 30, 90);
    }

    private double getRemap() throws RPCException, StreamException {
        return remap(1, 100, 90, 30, this.spaceShip.getHorizontalSpeed().get());
    }

}
