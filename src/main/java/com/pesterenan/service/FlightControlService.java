package com.pesterenan.service;

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

    public void setThrottle(BigDecimal throttle) throws RPCException {
        this.vessel.getControl().setThrottle(throttle.floatValue());
    }

    public double calcularTEP() throws RPCException, StreamException {
        return this.vessel.getAvailableThrust() / (this.spaceShip.getTotalShipMass().get() * this.spaceShip.getSurfaceGravity());
    }

    public double calcularAcelMaxima() throws RPCException, StreamException {
        return this.calcularTEP() * this.spaceShip.getSurfaceGravity() - this.spaceShip.getSurfaceGravity();
    }

}
