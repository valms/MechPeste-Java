package com.pesterenan.service;

import com.pesterenan.model.SpaceShip;
import com.pesterenan.utils.Utilities;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import krpc.client.RPCException;
import krpc.client.Stream;
import krpc.client.StreamException;
import krpc.client.services.SpaceCenter.AutoPilot;
import krpc.client.services.SpaceCenter.Engine;
import krpc.client.services.SpaceCenter.Node;
import krpc.client.services.SpaceCenter.Orbit;
import krpc.client.services.SpaceCenter.Vessel;
import lombok.extern.slf4j.Slf4j;
import org.javatuples.Triplet;

import static java.lang.Math.max;
import static java.lang.Math.sqrt;
import static java.math.BigDecimal.ZERO;

@Slf4j
public class ManeuverService {

    private final SpaceShip spaceShip;

    private final FlightControlService flightControlService;

    private final Vessel vessel;


    private final BigDecimal constGrav = new BigDecimal("9.81");

    public ManeuverService(SpaceShip spaceShip) {
        this.spaceShip = spaceShip;
        this.vessel = spaceShip.getActiveVessel();

        this.flightControlService = new FlightControlService(spaceShip);
    }

    public void apoapsissManeuver() throws RPCException {
        Orbit orbit = this.vessel.getOrbit();
        double gravitationalParameter = orbit.getBody().getGravitationalParameter();
        double initialApoapsis = orbit.getApoapsis();
        double timeToApoapsis = orbit.getTimeToApoapsis();
        double semiEixoMaior = orbit.getSemiMajorAxis();
        double velOrbitalAtual = sqrt(gravitationalParameter * ((2.0 / initialApoapsis) - (1.0 / semiEixoMaior)));
        double velOrbitalAlvo = sqrt(gravitationalParameter * ((2.0 / initialApoapsis) - (1.0 / initialApoapsis)));
        double deltaVdaManobra = velOrbitalAlvo - velOrbitalAtual;
        double[] deltaV = {deltaVdaManobra, 0, 0};
        criarManobra(timeToApoapsis, deltaV);
    }

    public void executeNextManeuver() {
        try {
            Node maneuverNode = this.vessel.getControl().getNodes().get(0);

            double burnTime = calculateBurnTime(maneuverNode);
            orientToManeuverNode(maneuverNode);
            executeBurn(maneuverNode, burnTime);
        } catch (UnsupportedOperationException | IndexOutOfBoundsException | RPCException e) {
            return;
        }
    }

    private void criarManobra(double tempoPosterior, double[] deltaV) {
        try {
            this.vessel.getControl().addNode(this.spaceShip.getSpaceCenter().getUT() + tempoPosterior, (float) deltaV[0],
                (float) deltaV[1], (float) deltaV[2]);
        } catch (UnsupportedOperationException | RPCException e) {
            return;
        }
    }


    private AutoPilot engageAutoPilot() {
        try {
            return this.vessel.getAutoPilot();
        } catch (RPCException e) {
            throw new RuntimeException(e);
        }
    }


    public void orientToManeuverNode(Node noDeManobra) {
        AutoPilot autoPilot = this.engageAutoPilot();
        try {

            float roll = autoPilot.getTargetRoll();

            autoPilot.setReferenceFrame(noDeManobra.getReferenceFrame());
            autoPilot.setTargetDirection(new Triplet<>(0.0, 1.0, 0.0));
            autoPilot.setTargetRoll((roll + 180));
            autoPilot.engage();

            while (autoPilot.getError() > 5) {
                log.info("Orientando nave para o nó de Manobra...");
                Thread.sleep(250);
            }

        } catch (InterruptedException | RPCException e) {
            try {
                autoPilot.setReferenceFrame(this.spaceShip.getPontoRefOrbital());
                autoPilot.disengage();
            } catch (RPCException e1) {
            }
            log.info("Não foi possível orientar a nave para a manobra:\n\t" + e.getMessage());
        }
    }

    public double calculateBurnTime(Node noDeManobra) throws RPCException {

        List<Engine> motores = this.vessel.getParts().getEngines();
        for (Engine motor : motores) {
            if (motor.getPart().getStage() == this.vessel.getControl().getCurrentStage() && motor.getActive() == false) {
                motor.setActive(true);
            }
        }
        double empuxo = this.vessel.getAvailableThrust();
        double isp = this.vessel.getSpecificImpulse() * this.constGrav.doubleValue();
        double massaTotal = this.vessel.getMass();
        double massaSeca = massaTotal / Math.exp(noDeManobra.getDeltaV() / isp);
        double taxaDeQueima = empuxo / isp;
        double duracaoDaQueima = (massaTotal - massaSeca) / taxaDeQueima;

        log.info("Tempo de Queima da Manobra: " + duracaoDaQueima + " segundos");
        return duracaoDaQueima;
    }

    public void executeBurn(Node noDeManobra, double duracaoDaQueima) throws RPCException {
        try {
            double inicioDaQueima = noDeManobra.getTimeTo() - (duracaoDaQueima / 2.0);
            log.info("Warp temporal para próxima manobra...");
            if (inicioDaQueima > 30) {
                this.spaceShip.getSpaceCenter().warpTo((this.spaceShip.getSpaceCenter().getUT() + inicioDaQueima - 10), 100000, 4);
            }

            // Mostrar tempo de ignição:
            log.info("Duração da queima: " + duracaoDaQueima + " segundos.");
            while (inicioDaQueima > 0) {
                inicioDaQueima = noDeManobra.getTimeTo() - (duracaoDaQueima / 2.0);
                inicioDaQueima = max(inicioDaQueima, 0.0);
                log.info(String.format("Ignição em: %1$.1f segundos...", inicioDaQueima));
                Thread.sleep(100);
            }
            // Executar a manobra:
            Stream<Triplet<Double, Double, Double>> queimaRestante = this.spaceShip.getKrpcConnection().addStream(noDeManobra,
                "remainingBurnVector", noDeManobra.getReferenceFrame());
            log.info("Executando manobra!");
            double limiteParaDesacelerar = noDeManobra.getDeltaV() > 1000 ? 0.025
                : noDeManobra.getDeltaV() > 250 ? 0.10 : 0.25;

            while (!Objects.isNull(noDeManobra)) {
                if (queimaRestante.get().getValue1() > 0.5) {
                    this.flightControlService.setThrottle(new BigDecimal(String.valueOf(Utilities.remap(noDeManobra.getDeltaV() * limiteParaDesacelerar, 0, 1, 0.1,
                        queimaRestante.get().getValue1()))));
                } else {
                    queimaRestante.remove();
                    break;
                }
//                MainGui.getParametros().getComponent(0).firePropertyChange("distancia", 0,
//                    queimaRestante.get().getValue1());
                Thread.sleep(25);
            }
            this.flightControlService.setThrottle(ZERO);
            this.vessel.getAutoPilot().setReferenceFrame(this.spaceShip.getPontoRefSuperficie());
            this.vessel.getAutoPilot().disengage();
            this.vessel.getControl().setSAS(true);
            this.vessel.getControl().setRCS(false);
            noDeManobra.remove();
//            StatusJPanel.setStatus(Status.PRONTO.get());
        } catch (StreamException | RPCException e) {
            this.flightControlService.setThrottle(ZERO);
            this.vessel.getAutoPilot().disengage();
//            StatusJPanel.setStatus("Não foi possivel buscar os dados da nave.");
        } catch (InterruptedException e) {
            this.flightControlService.setThrottle(ZERO);
            this.vessel.getAutoPilot().disengage();
//            StatusJPanel.setStatus("Manobra cancelada.");
        }
    }

}
