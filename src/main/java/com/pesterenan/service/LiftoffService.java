package com.pesterenan.service;

import com.pesterenan.enums.Modulos;
import com.pesterenan.exceptions.FlightControlException;
import com.pesterenan.exceptions.LiftoffException;
import com.pesterenan.model.SpaceShip;
import com.pesterenan.utils.ControlePID;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import krpc.client.RPCException;
import krpc.client.StreamException;
import krpc.client.services.SpaceCenter.Engine;
import krpc.client.services.SpaceCenter.Part;
import krpc.client.services.SpaceCenter.Vessel;
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
public class LiftoffService implements Runnable {

    private final MathContext mathContext = DECIMAL64;

    private final SpaceShip spaceShip;

    private final FlightControlService flightControlService;

    private final Vessel vessel;

    private final ControlePID controlePID;

    // Controls Variables
    private final BigDecimal heading = new BigDecimal("90");
    private final BigDecimal roll = new BigDecimal("90");
    private final BigDecimal finalApoapsisAltitude = new BigDecimal("80000");
    private final BigDecimal startCurveAlt = new BigDecimal("100");

    //Modulos TODO: Rever Estrutura
    private final String gravityCurveModel = Modulos.CIRCULAR.get();

    public LiftoffService(SpaceShip spaceShip) {
        this.spaceShip = spaceShip;
        this.vessel = spaceShip.getActiveVessel();

        this.controlePID = new ControlePID();
        this.flightControlService = new FlightControlService(spaceShip);
    }

    @Override
    public void run() {
        try {
            this.executeLiftoffProcedure();
            this.gravityCurve();
        } catch (LiftoffException e) {
            throw new RuntimeException(e);
        }
    }

    public void executeLiftoffProcedure() throws LiftoffException {
        try {
            this.vessel.getControl().setSAS(true);
            this.flightControlService.setThrottle(ONE); // Full Throttle

            if (this.vessel.getSituation().equals(PRE_LAUNCH)) {
                this.doLaunchCountdown();
                this.vessel.getControl().activateNextStage();
            }
        } catch (FlightControlException | InterruptedException | RPCException e) {
            throw new LiftoffException("Erro ao executar o programa de lançamento!");
        }
    }

    public void gravityCurve() throws LiftoffException {
        try {

            final BigDecimal PITCH_UP = new BigDecimal("90");
            BigDecimal currentPitch = PITCH_UP;

            this.prepareFlight(currentPitch);

            while (currentPitch.compareTo(ONE) > 0) {
                this.separateStageWithousFuel();

                if (this.isApoapsisReached()) {
                    break;
                }

                double currentAltitude = remap(this.startCurveAlt.doubleValue(), this.finalApoapsisAltitude.doubleValue(),
                    1, 0.01, this.spaceShip.getMeanAltitude().get());

                BigDecimal inclinationCurve = this.calculateInclinationCurve(currentAltitude);
                currentPitch = inclinationCurve.multiply(PITCH_UP, this.mathContext);

                this.vessel.getAutoPilot().targetPitchAndHeading(currentPitch.floatValue(), this.heading.floatValue());
                double pid = this.controlePID.computarPID(this.spaceShip.getApoapsisAltitude().get(), this.finalApoapsisAltitude.doubleValue());

                log.info("pid: {}", pid);
                this.flightControlService.setThrottle(new BigDecimal(valueOf(pid)));

                log.info("A inclinação do foguete é: {}", currentPitch);
                Thread.sleep(250);
            }
        } catch (StreamException | FlightControlException | InterruptedException | RPCException e) {
            throw new LiftoffException("Erro ao executar o programa de lançamento!");
        }
    }

    public void finalizeOrbit() throws RPCException, StreamException, InterruptedException, FlightControlException {
        this.vessel.getAutoPilot().disengage();
        this.vessel.getControl().setSAS(true);
        this.vessel.getControl().setRCS(true);

        while (this.spaceShip.getFlightParameters().getStaticPressure() > 100) {
            this.vessel.getAutoPilot().setTargetDirection(this.spaceShip.getFlightParameters().getPrograde());
            double throttleValue = controlePID.computarPID(this.spaceShip.getApoapsisAltitude().get(), this.finalApoapsisAltitude.doubleValue());
            this.flightControlService.setThrottle(new BigDecimal(valueOf(throttleValue)));
            Thread.sleep(500);
        }

        Map<String, String> commands = new HashMap<>();
        commands.put(Modulos.MODULO.get(), Modulos.MODULO_MANOBRAS.get());
        commands.put(Modulos.FUNCAO.get(), Modulos.APOASTRO.get());
//        MechPeste.iniciarModulo(commands);

    }

    private void prepareFlight(BigDecimal currentPitch) throws RPCException, FlightControlException {
        this.vessel.getAutoPilot().engage();
        this.vessel.getAutoPilot().targetPitchAndHeading(currentPitch.floatValue(), this.heading.floatValue());
        this.vessel.getAutoPilot().setTargetRoll(this.roll.floatValue());
        this.flightControlService.setThrottle(ONE);
    }

    private void separateStageWithousFuel() throws RPCException, InterruptedException {
        int currentStage = this.vessel.getControl().getCurrentStage();
        List<Part> parts = this.vessel.getParts().inStage(currentStage);
        boolean haveFuel = parts.stream().map(this::handlePartsWithFuel).findAny().orElse(true);

        if (!haveFuel) {
            log.info("Separando o estágio {}", currentStage);
            Thread.sleep(1000);
            this.separeteStages();
        }
    }

    private boolean handlePartsWithFuel(Part part) {
        try {
            log.info("Tem Combustivel {}", part.getEngine().getHasFuel());
            return part.getEngine().getHasFuel();

        } catch (RPCException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isApoapsisReached() throws RPCException, StreamException {
        return this.spaceShip.getApoapsisAltitude().get() >= this.finalApoapsisAltitude.doubleValue();
    }

    public void separeteStages() throws RPCException {
        this.vessel.getControl().activateNextStage();
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
