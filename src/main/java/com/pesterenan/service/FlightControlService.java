package com.pesterenan.service;

import com.pesterenan.exceptions.FlightControlException;
import com.pesterenan.model.SpaceShip;
import java.math.BigDecimal;
import krpc.client.RPCException;
import krpc.client.StreamException;
import krpc.client.services.SpaceCenter.Vessel;

public class FlightControlService {

    private final SpaceShip spaceShip;

    private final Vessel vessel;

    public FlightControlService(SpaceShip spaceShip) {
        this.spaceShip = spaceShip;
        this.vessel = spaceShip.getActiveVessel();
    }

    public void setThrottle(BigDecimal throttle) throws FlightControlException {
        try {
            this.vessel.getControl().setThrottle(throttle.floatValue());
        } catch (RPCException ex) {
            ex.printStackTrace();
            throw new FlightControlException("Erro ao regular o acelerador!");
        }
    }

    public double calcularTEP() throws FlightControlException {
        try {
            return this.vessel.getAvailableThrust() / (this.spaceShip.getTotalShipMass().get() * this.spaceShip.getSurfaceGravity());
        } catch (RPCException | StreamException ex) {
            ex.printStackTrace();
            throw new FlightControlException("Erro ao calcular o TEP!");
        }
    }

    public double calcularAcelMaxima() throws FlightControlException {
        try {
            return this.calcularTEP() * this.spaceShip.getSurfaceGravity() - this.spaceShip.getSurfaceGravity();
        } catch (FlightControlException ex) {
            ex.printStackTrace();
            throw new FlightControlException("Erro ao calcular a aceleração!");
        }

    }

}
