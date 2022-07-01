package com.pesterenan.service;

import com.pesterenan.enums.Modulos;
import com.pesterenan.model.SpaceShip;
import com.pesterenan.utils.ControlePID;
import java.math.BigDecimal;
import java.math.MathContext;
import krpc.client.RPCException;
import krpc.client.StreamException;
import krpc.client.services.SpaceCenter.Engine;
import krpc.client.services.SpaceCenter.Vessel;
import lombok.extern.java.Log;
import lombok.extern.log4j.Log4j;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;

import static com.pesterenan.utils.Utilities.easeInCirc;
import static com.pesterenan.utils.Utilities.easeInCubic;
import static com.pesterenan.utils.Utilities.easeInExpo;
import static com.pesterenan.utils.Utilities.easeInQuad;
import static com.pesterenan.utils.Utilities.easeInSine;
import static com.pesterenan.utils.Utilities.remap;
import static java.lang.String.valueOf;
import static java.math.BigDecimal.ONE;
import static java.math.MathContext.DECIMAL64;
import static krpc.client.services.SpaceCenter.VesselSituation.PRE_LAUNCH;

@Slf4j
public class LiftoffService {

    private final MathContext mathContext = DECIMAL64;

    private final SpaceShip spaceShip;

    private final FlightControlService flightControlService;

    private final Vessel vessel;

    private final ControlePID controlePID;

    // Controls Variables
    private BigDecimal heading = new BigDecimal("90");
    private BigDecimal roll = new BigDecimal("90");
    private BigDecimal finalApoapsisAltitude = new BigDecimal("80000");
    private BigDecimal startCurveAlt = new BigDecimal("100");

    //Modulos TODO: Rever Estrutura
    private String gravityCurveModel = Modulos.CIRCULAR.get();

    public LiftoffService(SpaceShip spaceShip) {
        this.spaceShip = spaceShip;
        this.vessel = spaceShip.getActiveVessel();

        this.controlePID = new ControlePID();
        this.flightControlService = new FlightControlService(spaceShip);
    }

    public void executeLiftoffProcedure() throws RPCException, InterruptedException {
        log.info("executeLiftoffProcedure");
        this.vessel.getControl().setSAS(true);
        this.flightControlService.setThrottle(ONE); // Full Throttle

        if (this.vessel.getSituation().equals(PRE_LAUNCH)) {
            this.doLaunchCountdown();
            this.vessel.getControl().activateNextStage();
        }
    }

    public synchronized void gravityCurve() throws RPCException, InterruptedException, StreamException {
        final BigDecimal PITCH_UP = new BigDecimal("90");
        BigDecimal currentPitch = PITCH_UP;

        this.vessel.getAutoPilot().targetPitchAndHeading(currentPitch.floatValue(), this.heading.floatValue());
        this.vessel.getAutoPilot().setTargetRoll(this.roll.floatValue());
        this.vessel.getAutoPilot().engage();

        this.flightControlService.setThrottle(ONE);

        while (currentPitch.compareTo(ONE) > 0) {
            if (this.spaceShip.getApoapsisAltitude().get() > this.finalApoapsisAltitude.doubleValue()) {
                break;
            }

            double currentAltitude = remap(this.startCurveAlt.doubleValue(), this.finalApoapsisAltitude.doubleValue(),
                1, 0.01, this.spaceShip.getMeanAltitude().get());

            BigDecimal inclinationCurve = this.calculateInclinationCurve(currentAltitude);

            currentPitch = inclinationCurve.multiply(PITCH_UP, this.mathContext);

            this.vessel.getAutoPilot().targetPitchAndHeading(currentPitch.floatValue(), this.heading.floatValue());

            double pid = this.controlePID.computarPID(this.spaceShip.getApoapsisAltitude().get(), this.finalApoapsisAltitude.doubleValue());
            this.flightControlService.setThrottle(new BigDecimal(valueOf(pid)));

            if (stageWithoutFuel()) {
                System.out.println("Separando estágio...");
                Thread.sleep(1000);
                this.vessel.getControl().activateNextStage();
                Thread.sleep(1000);
            }

            System.out.printf("A inclinação do foguete é: %.1f%n", currentPitch);
            Thread.sleep(250);
        }
    }

    private BigDecimal calculateInclinationCurve(double currentAltitude) {
        if (this.gravityCurveModel.equals(Modulos.QUADRATICA.get())) {
            return new BigDecimal(valueOf(easeInQuad(currentAltitude)));
        }

        if (this.gravityCurveModel.equals(Modulos.CUBICA.get())) {
            return new BigDecimal(valueOf(easeInCubic(currentAltitude)));
        }

        if (this.gravityCurveModel.equals(Modulos.SINUSOIDAL.get())) {
            return new BigDecimal(valueOf(easeInSine(currentAltitude)));
        }

        if (this.gravityCurveModel.equals(Modulos.EXPONENCIAL.get())) {
            return new BigDecimal(valueOf(easeInExpo(currentAltitude)));
        }

        return new BigDecimal(valueOf(easeInCirc(currentAltitude)));
    }

    private boolean stageWithoutFuel() throws RPCException, StreamException {
        for (Engine motor : this.vessel.getParts().getEngines()) {
            if (motor.getPart().getStage() == this.vessel.getControl().getCurrentStage() && !motor.getHasFuel()) {
                return true;
            }
        }

        return false;
    }

    private void doLaunchCountdown() throws InterruptedException {
        double countdown = 5;
        while (countdown > 0) {
            countdown -= 0.1;
            Thread.sleep(100);
        }
    }

}
