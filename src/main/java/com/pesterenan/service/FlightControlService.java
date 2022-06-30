package com.pesterenan.service;

import com.pesterenan.model.SpaceShip;
import com.pesterenan.views.StatusJPanel;
import java.math.BigDecimal;
import krpc.client.RPCException;
import krpc.client.StreamException;
import krpc.client.services.SpaceCenter.Vessel;

import static java.math.BigDecimal.ONE;
import static krpc.client.services.SpaceCenter.VesselSituation.PRE_LAUNCH;

public class FlightControlService {

    private final SpaceShip spaceShip;

    private final StatusJPanel statusJPanel;

    private final Vessel vessel;

    public FlightControlService(SpaceShip spaceShip, StatusJPanel statusJPanel) {
        this.spaceShip = spaceShip;
        this.statusJPanel = statusJPanel;
        this.vessel = spaceShip.getActiveVessel();
    }

    public void setThrottle(BigDecimal throttle) throws RPCException {
        this.vessel.getControl().setThrottle(throttle.floatValue());
    }

    public void executeLiftoffProcedure() throws RPCException, InterruptedException {
        this.vessel.getControl().setSAS(true);
        this.setThrottle(ONE); // Full Throttle

        if (this.vessel.getSituation().equals(PRE_LAUNCH)) {
            this.doLaunchCountdown();
            this.vessel.getControl().activateNextStage();
        }

        //TODO: Isolar essa chamada da View em uma camada superior (Controller)
        this.statusJPanel.setStatusLbl("Decolagem!");
    }

    public double calcularTEP() throws RPCException, StreamException {
        return this.vessel.getAvailableThrust() / (this.spaceShip.getMassaTotal().get() * this.spaceShip.getAcelGravidade());
    }

    public double calcularAcelMaxima() throws RPCException, StreamException {
        return this.calcularTEP() * this.spaceShip.getAcelGravidade() - this.spaceShip.getAcelGravidade();
    }

    private void doLaunchCountdown() throws InterruptedException {
        double countdown = 5;
        while (countdown > 0) {
            statusJPanel.setStatusLbl(String.format("Lançamento em: %.1f segundos...", countdown));
            countdown -= 0.1;
            Thread.sleep(100);
        }
    }

}
